package com.itcotato.dortfolio.domain.insight.service;

import com.itcotato.dortfolio.domain.insight.dto.res.LatestInsightResponse;
import com.itcotato.dortfolio.domain.insight.dto.res.LatestInsightResponse.CurrentGenerationResponse;
import com.itcotato.dortfolio.domain.insight.dto.res.LatestInsightResponse.ChangeSummaryResponse;
import com.itcotato.dortfolio.domain.insight.dto.res.LatestInsightResponse.JobRecommendationResponse;
import com.itcotato.dortfolio.domain.insight.dto.res.LatestInsightResponse.JobSnapshotResponse;
import com.itcotato.dortfolio.domain.insight.dto.res.LatestInsightResponse.StrengthRecordResponse;
import com.itcotato.dortfolio.domain.insight.dto.res.LatestInsightResponse.StrengthResponse;
import com.itcotato.dortfolio.domain.insight.dto.res.LatestInsightResponse.TemplateStatisticResponse;
import com.itcotato.dortfolio.domain.insight.entity.Insight;
import com.itcotato.dortfolio.domain.insight.entity.InsightJobRecommendation;
import com.itcotato.dortfolio.domain.insight.entity.InsightStrength;
import com.itcotato.dortfolio.domain.insight.entity.InsightStrengthRecord;
import com.itcotato.dortfolio.domain.insight.entity.InsightTemplateStatistic;
import com.itcotato.dortfolio.domain.insight.repository.InsightJobRecommendationRepository;
import com.itcotato.dortfolio.domain.insight.repository.InsightRepository;
import com.itcotato.dortfolio.domain.insight.repository.InsightStrengthRecordRepository;
import com.itcotato.dortfolio.domain.insight.repository.InsightStrengthRepository;
import com.itcotato.dortfolio.domain.insight.repository.InsightTemplateStatisticRepository;
import com.itcotato.dortfolio.domain.insight.repository.InsightRecordQueryRepository;
import com.itcotato.dortfolio.domain.record.analysis.entity.AiAnalysisStatus;
import com.itcotato.dortfolio.domain.record.repository.RecordRepository;
import com.itcotato.dortfolio.domain.user.repository.UserJobRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.InsightErrorCode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LatestInsightQueryService {

    private final InsightRepository insightRepository;
    private final InsightStrengthRepository strengthRepository;
    private final InsightStrengthRecordRepository strengthRecordRepository;
    private final InsightTemplateStatisticRepository templateRepository;
    private final InsightJobRecommendationRepository recommendationRepository;
    private final RecordRepository recordRepository;
    private final InsightRecordQueryRepository insightRecordQueryRepository;
    private final UserJobRepository userJobRepository;

    public LatestInsightResponse getLatest(UUID userId) {
        /* 항상 가장 최근 COMPLETED Insight만 조회 */
        Insight insight = insightRepository
                .findLatestCompletedByUserId(userId)
                .orElseThrow(() -> new CustomException(
                        InsightErrorCode.INSIGHT_NOT_FOUND
                ));

        UUID insightId = insight.getId();

        List<InsightStrength> strengths =
                strengthRepository
                        .findAllByInsight_IdOrderByRankAscStrengthTagIdSnapshotAsc(
                                insightId
                        );

        List<InsightStrengthRecord> strengthRecords =
                strengthRecordRepository.findAllForInsight(
                        insightId
                );

        List<InsightTemplateStatistic> templates =
                templateRepository
                        .findAllByInsight_IdOrderByRankAscTemplateIdSnapshotAsc(
                                insightId
                        );

        List<InsightJobRecommendation> recommendations =
                recommendationRepository
                        .findAllByInsight_IdOrderBySortOrderSnapshotAscJobCompetencyIdSnapshotAsc(
                                insightId
                        );

        /* 강점 연결 기록과 추천 기록의 모든 원본 record ID를 합침 */
        Set<UUID> referencedRecordIds =
                collectReferencedRecordIds(
                        strengthRecords,
                        recommendations
                );

        /* 빈 IN 조건은 DB마다 동작이 달라질 수 있으므로, ID가 없으면 Repository를 호출 X */
        Set<UUID> availableRecordIds =
                referencedRecordIds.isEmpty()
                        ? Set.of()
                        : recordRepository.findAvailableRecordIds(
                        userId,
                        referencedRecordIds
                );

        Map<UUID, List<InsightStrengthRecord>>
                recordsByStrengthId =
                strengthRecords.stream()
                        .collect(Collectors.groupingBy(
                                strengthRecord ->
                                        strengthRecord
                                                .getInsightStrength()
                                                .getId()
                        ));

        List<StrengthResponse> strengthResponses =
                strengths.stream()
                        .map(strength ->
                                toStrengthResponse(
                                        strength,
                                        recordsByStrengthId.getOrDefault(
                                                strength.getId(),
                                                List.of()
                                        ),
                                        availableRecordIds
                                )
                        )
                        .toList();

        List<TemplateStatisticResponse> templateResponses =
                templates.stream()
                        .map(this::toTemplateResponse)
                        .toList();

        List<JobRecommendationResponse> recommendationResponses =
                recommendations.stream()
                        .map(recommendation ->
                                toRecommendationResponse(
                                        recommendation,
                                        availableRecordIds
                                )
                        )
                        .toList();

        CurrentGenerationResponse currentGeneration =
                insightRepository.findPendingByUserId(userId)
                        .map(this::toCurrentGenerationResponse)
                        .orElse(null);

        ChangeSummaryResponse changes = new ChangeSummaryResponse(
                insightRecordQueryRepository.countEligibleRecordsAnalyzedAfter(
                        userId,
                        insight.getRecordSnapshotAt()
                ),
                userJobRepository.findByUserIdAndIsPrimaryTrue(userId)
                        .map(userJob -> !userJob.getJob().getId()
                                .equals(insight.getJobIdSnapshot()))
                        .orElse(true),
                insightRecordQueryRepository.countRecordsByAnalysisStatus(
                        userId,
                        AiAnalysisStatus.PENDING
                ),
                insightRecordQueryRepository.countRecordsByAnalysisStatus(
                        userId,
                        AiAnalysisStatus.FAILED
                )
        );

        return new LatestInsightResponse(
                insight.getId(),
                new JobSnapshotResponse(
                        insight.getJobIdSnapshot(),
                        insight.getJobNameSnapshot()
                ),
                insight.getRecordSnapshotAt(),
                insight.getCompletedAt(),
                insight.getBaseCompletedRecordCount(),
                strengthResponses,
                templateResponses,
                recommendationResponses,
                changes,
                currentGeneration
        );
    }

    private Set<UUID> collectReferencedRecordIds(
            Collection<InsightStrengthRecord> strengthRecords,
            Collection<InsightJobRecommendation> recommendations
    ) {
        Stream<UUID> strengthRecordIds =
                strengthRecords.stream()
                        .map(
                                InsightStrengthRecord
                                        ::getRecordIdSnapshot
                        );

        Stream<UUID> recommendationRecordIds =
                recommendations.stream()
                        .map(
                                InsightJobRecommendation
                                        ::getRecordIdSnapshot
                        )
                        .filter(Objects::nonNull);

        return Stream.concat(
                        strengthRecordIds,
                        recommendationRecordIds
                )
                .collect(Collectors.toSet());
    }

    private StrengthResponse toStrengthResponse(
            InsightStrength strength,
            List<InsightStrengthRecord> records,
            Set<UUID> availableRecordIds
    ) {
        List<StrengthRecordResponse> recordResponses =
                records.stream()
                        .map(record ->
                                new StrengthRecordResponse(
                                        record.getRecordIdSnapshot(),
                                        record.getRecordTitleSnapshot(),
                                        record.getRecordCompletedAtSnapshot(),
                                        availableRecordIds.contains(
                                                record.getRecordIdSnapshot()
                                        )
                                )
                        )
                        .toList();

        return new StrengthResponse(
                strength.getStrengthTagIdSnapshot(),
                strength.getStrengthNameSnapshot(),
                strength.getRecordCount(),
                strength.getAverageScore(),
                strength.getRatio(),
                strength.getRank(),
                recordResponses
        );
    }

    private TemplateStatisticResponse toTemplateResponse(
            InsightTemplateStatistic template
    ) {
        return new TemplateStatisticResponse(
                template.getTemplateIdSnapshot(),
                template.getTemplateNameSnapshot(),
                template.getRecordCount(),
                template.getRatio(),
                template.getRank()
        );
    }

    private JobRecommendationResponse toRecommendationResponse(
            InsightJobRecommendation recommendation,
            Set<UUID> availableRecordIds
    ) {
        UUID recordId = recommendation.getRecordIdSnapshot();

        return new JobRecommendationResponse(
                recommendation.getJobCompetencyIdSnapshot(),
                recommendation.getCompetencyNameSnapshot(),
                recommendation.getMatchStatus(),
                recordId,
                recommendation.getRecordTitleSnapshot(),
                recommendation.getTemplateNameSnapshot(),
                recommendation.getReason(),
                recommendation.getSimilarity(),
                recordId != null && availableRecordIds.contains(recordId)
        );
    }

    private CurrentGenerationResponse toCurrentGenerationResponse(
            Insight pending
    ) {
        return new CurrentGenerationResponse(
                pending.getId(),
                pending.getStatus(),
                pending.getRequestedAt()
        );
    }
}
