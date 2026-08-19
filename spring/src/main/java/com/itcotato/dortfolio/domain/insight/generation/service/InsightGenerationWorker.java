package com.itcotato.dortfolio.domain.insight.generation.service;

import com.itcotato.dortfolio.domain.insight.config.InsightProperties;
import com.itcotato.dortfolio.domain.insight.generation.config.InsightGenerationAsyncConfig;
import com.itcotato.dortfolio.domain.insight.generation.lock.InsightGenerationLock;
import com.itcotato.dortfolio.domain.insight.generation.model.InsightGenerationCommand;
import com.itcotato.dortfolio.domain.insight.generation.model.InsightGenerationCommand.JobCompetencySnapshot;
import com.itcotato.dortfolio.domain.insight.generation.model.InsightGenerationResult;
import com.itcotato.dortfolio.domain.insight.generation.model.InsightGenerationResult.JobRecommendationResult;
import com.itcotato.dortfolio.domain.insight.generation.model.InsightGenerationResult.StrengthRecordResult;
import com.itcotato.dortfolio.domain.insight.generation.model.InsightGenerationResult.StrengthResult;
import com.itcotato.dortfolio.domain.insight.generation.model.InsightGenerationResult.TemplateResult;
import com.itcotato.dortfolio.domain.insight.query.AnalyzedRecordQuery;
import com.itcotato.dortfolio.domain.insight.query.AnalyzedRecordSnapshot;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationCandidate;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationCompetency;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationRequest;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationResult;
import com.itcotato.dortfolio.domain.insight.recommendation.repository.RecommendationCandidateQuery;
import com.itcotato.dortfolio.domain.insight.recommendation.service.InsightRecommendationGenerator;
import com.itcotato.dortfolio.domain.insight.statistics.StrengthStatistic;
import com.itcotato.dortfolio.domain.insight.statistics.StrengthStatisticsCalculator;
import com.itcotato.dortfolio.domain.insight.statistics.TemplateDistributionCalculator;
import com.itcotato.dortfolio.domain.insight.statistics.TemplateStatistic;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.InsightErrorCode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/* Insight 생성 계산을 비동기로 수행 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InsightGenerationWorker {

    /* 강점별 연결 기록의 표시 순서를 고정 */
    private static final Comparator<AnalyzedRecordSnapshot>
            STRENGTH_RECORD_COMPARATOR =
            Comparator.comparing(
                            AnalyzedRecordSnapshot::completedAt
                    )
                    .reversed()
                    .thenComparing(
                            AnalyzedRecordSnapshot::recordId
                    );

    private final AnalyzedRecordQuery analyzedRecordQuery;
    private final StrengthStatisticsCalculator strengthCalculator;
    private final TemplateDistributionCalculator templateCalculator;
    private final RecommendationCandidateQuery candidateQuery;
    private final InsightRecommendationGenerator recommendationGenerator;
    private final InsightGenerationResultWriter resultWriter;
    private final InsightGenerationRunningWriter runningWriter;
    private final InsightGenerationFailureWriter failureWriter;
    private final InsightGenerationLock generationLock;
    private final InsightProperties insightProperties;

    /* 지정된 전용 Executor에서 Insight 생성을 실행 */
    @Async(InsightGenerationAsyncConfig.EXECUTOR_NAME)
    public void generate(
            InsightGenerationCommand command,
            String lockToken
    ) {
        try {
            runningWriter.markRunning(command.insightId());

            InsightGenerationResult result = generateResult(command);

            // 모든 계산이 성공한 경우에만 결과 저장을 요청합니다.
            resultWriter.complete(result);
        } catch (Exception exception) {
            markFailed(command.insightId(), exception);
        } finally {
            // 성공과 실패 여부에 상관없이 반드시 락을 해제
            generationLock.release(
                    command.userId(),
                    lockToken
            );
        }
    }

    private InsightGenerationResult generateResult(
            InsightGenerationCommand command
    ) {
        List<AnalyzedRecordSnapshot> records =
                analyzedRecordQuery.findAllForInsight(
                        command.userId(),
                        command.snapshotAt()
                );

        if (records.isEmpty()) {
            throw new CustomException(
                    InsightErrorCode
                            .INSIGHT_ANALYZED_RECORDS_EMPTY
            );
        }

        List<StrengthStatistic> strengthStatistics =
                strengthCalculator.calculate(records);

        List<TemplateStatistic> templateStatistics =
                templateCalculator.calculate(records);

        List<StrengthResult> strengths =
                toStrengthResults(
                        strengthStatistics,
                        records
                );

        List<TemplateResult> templates =
                toTemplateResults(templateStatistics);

        List<JobRecommendationResult> recommendations =
                generateRecommendations(command, records.size());

        return new InsightGenerationResult(
                command.insightId(),
                strengths,
                templates,
                recommendations
        );
    }

    private List<StrengthResult> toStrengthResults(
            List<StrengthStatistic> statistics,
            List<AnalyzedRecordSnapshot> records
    ) {
        return statistics.stream()
                .map(statistic -> new StrengthResult(
                        statistic.tagId(),
                        statistic.tagName(),
                        Math.toIntExact(
                                statistic.recordCount()
                        ),
                        statistic.averageScore(),
                        statistic.ratio(),
                        statistic.rank(),
                        findStrengthRecords(
                                statistic.tagId(),
                                records
                        )
                ))
                .toList();
    }

    /* 해당 강점 태그를 가진 기록을 찾아 스냅샷으로 변환 */
    private List<StrengthRecordResult> findStrengthRecords(
            UUID strengthTagId,
            List<AnalyzedRecordSnapshot> records
    ) {
        return records.stream()
                .filter(record ->
                        record.strengthTags().stream()
                                .anyMatch(tag ->
                                        tag.tagId().equals(
                                                strengthTagId
                                        )
                                )
                )
                .sorted(STRENGTH_RECORD_COMPARATOR)
                .map(record -> new StrengthRecordResult(
                        record.recordId(),
                        record.recordTitle(),
                        record.completedAt()
                ))
                .toList();
    }

    private List<TemplateResult> toTemplateResults(
            List<TemplateStatistic> statistics
    ) {
        return statistics.stream()
                .map(statistic -> new TemplateResult(
                        statistic.templateId(),
                        statistic.templateName(),
                        Math.toIntExact(
                                statistic.recordCount()
                        ),
                        statistic.ratio(),
                        statistic.rank()
                ))
                .toList();
    }

    /* 스냅샷에 포함된 5개 직무 역량을 순서대로 처리 */
    private List<JobRecommendationResult> generateRecommendations(
            InsightGenerationCommand command,
            int totalEligibleRecordCount
    ) {
        int candidateCount = calculateCandidateCount(totalEligibleRecordCount);

        List<RecommendationCompetency> competencies = command.jobCompetencies()
                .stream()
                .map(competency -> new RecommendationCompetency(
                        competency.jobCompetencyId(),
                        competency.competencyName(),
                        competency.competencyDescription(),
                        candidateQuery.findTopCandidates(
                                command.userId(),
                                competency.jobCompetencyId(),
                                command.snapshotAt(),
                                candidateCount,
                                insightProperties.recommendationMinSimilarity()
                        )
                ))
                .toList();

        RecommendationRequest request = new RecommendationRequest(
                command.jobId(),
                command.jobName(),
                competencies
        );
        List<RecommendationResult> recommendations =
                recommendationGenerator.generate(request);
        Map<UUID, RecommendationCompetency> competenciesById =
                competencies.stream().collect(Collectors.toMap(
                        RecommendationCompetency::jobCompetencyId,
                        Function.identity()
                ));

        return recommendations.stream()
                .map(recommendation -> toRecommendationResult(
                        competenciesById.get(recommendation.jobCompetencyId()),
                        recommendation
                ))
                .toList();
    }

    int calculateCandidateCount(int totalEligibleRecordCount) {
        return Math.min(insightProperties.recommendationCandidateMax(), Math.max(insightProperties.recommendationCandidateMin(), (int) Math.ceil(totalEligibleRecordCount * insightProperties.recommendationCandidateRatio())));
    }

    private JobRecommendationResult toRecommendationResult(
            RecommendationCompetency competency,
            RecommendationResult recommendation
    ) {
        if (!recommendation.matched()) {
            return JobRecommendationResult.noMatch(
                    competency.jobCompetencyId(),
                    competency.competencyName()
            );
        }

        RecommendationCandidate selectedCandidate =
                competency.candidates().stream()
                        .filter(candidate ->
                                candidate.recordId().equals(
                                        recommendation.recordId()
                                )
                        )
                        .findFirst()
                        .orElseThrow(() -> new CustomException(
                                InsightErrorCode
                                        .INSIGHT_RECOMMENDATION_INVALID_RESPONSE
                        ));

        return new JobRecommendationResult(
                true,
                competency.jobCompetencyId(),
                competency.competencyName(),
                selectedCandidate.recordId(),
                selectedCandidate.recordTitle(),
                selectedCandidate.templateName(),
                recommendation.reason(),
                selectedCandidate.similarity()
        );
    }

    private void markFailed(
            UUID insightId,
            Exception generationException
    ) {
        FailureInformation failure =
                toFailureInformation(generationException);

        try {
            failureWriter.fail(
                    insightId,
                    failure.code(),
                    failure.message()
            );
        } catch (Exception failureWritingException) {
            // FAILED 저장까지 실패했다면 운영자가 추적할 수 있도록 두 예외 모두 기록
            log.error(
                    "Failed to persist Insight generation failure. "
                            + "insightId={}, originalFailureCode={}",
                    insightId,
                    failure.code(),
                    failureWritingException
            );
        }

        log.warn(
                "Insight generation failed. "
                        + "insightId={}, failureCode={}, exceptionType={}",
                insightId,
                failure.code(),
                generationException.getClass().getSimpleName(),
                generationException
        );
    }

    /* CustomException은 정의된 오류 코드를 그대로 저장 */
    private FailureInformation toFailureInformation(
            Exception exception
    ) {
        if (exception instanceof CustomException customException) {
            return new FailureInformation(
                    customException.getErrorCode().getCode(),
                    customException.getErrorCode().getMessage()
            );
        }

        return new FailureInformation(
                InsightErrorCode
                        .INSIGHT_GENERATION_FAILED
                        .getCode(),
                InsightErrorCode
                        .INSIGHT_GENERATION_FAILED
                        .getMessage()
        );
    }

    private record FailureInformation(
            String code,
            String message
    ) {
    }
}
