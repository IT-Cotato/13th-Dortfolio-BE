package com.itcotato.dortfolio.domain.insight.generation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itcotato.dortfolio.domain.insight.config.InsightProperties;
import com.itcotato.dortfolio.domain.insight.generation.lock.InsightGenerationLock;
import com.itcotato.dortfolio.domain.insight.generation.model.InsightGenerationCommand;
import com.itcotato.dortfolio.domain.insight.generation.model.InsightGenerationCommand.JobCompetencySnapshot;
import com.itcotato.dortfolio.domain.insight.generation.model.InsightGenerationResult;
import com.itcotato.dortfolio.domain.insight.query.AnalyzedRecordQuery;
import com.itcotato.dortfolio.domain.insight.query.AnalyzedRecordSnapshot;
import com.itcotato.dortfolio.domain.insight.query.AnalyzedRecordSnapshot.StrengthTagSnapshot;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationCandidate;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InsightGenerationWorkerTest {

    private static final LocalDateTime SNAPSHOT_AT =
            LocalDateTime.of(2026, 8, 6, 10, 0);

    private static final UUID INSIGHT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID JOB_ID = UUID.randomUUID();

    @Mock
    private AnalyzedRecordQuery analyzedRecordQuery;

    @Mock
    private StrengthStatisticsCalculator strengthCalculator;

    @Mock
    private TemplateDistributionCalculator templateCalculator;

    @Mock
    private RecommendationCandidateQuery candidateQuery;

    @Mock
    private InsightRecommendationGenerator recommendationGenerator;

    @Mock
    private InsightGenerationResultWriter resultWriter;

    @Mock
    private InsightGenerationRunningWriter runningWriter;

    @Mock
    private InsightGenerationFailureWriter failureWriter;

    @Mock
    private InsightGenerationLock generationLock;

    private InsightGenerationWorker worker;

    @BeforeEach
    void setUp() {
        InsightProperties properties = new InsightProperties(
                10,
                Duration.ofHours(24),
                0.1,
                1,
                5,
                0.0,
                "gemini-embedding-2",
                2,
                Duration.ZERO,
                Duration.ofSeconds(10)
        );

        worker = new InsightGenerationWorker(
                analyzedRecordQuery,
                strengthCalculator,
                templateCalculator,
                candidateQuery,
                recommendationGenerator,
                resultWriter,
                runningWriter,
                failureWriter,
                generationLock,
                properties
        );
    }

    @Test
    void calculatesCandidateCountWithRatioAndBounds() {
        assertThat(worker.calculateCandidateCount(1)).isEqualTo(1);
        assertThat(worker.calculateCandidateCount(30)).isEqualTo(3);
        assertThat(worker.calculateCandidateCount(100)).isEqualTo(5);
    }

    @Test
    void calculatesAndStoresInsightResult() {
        // given
        UUID tagId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID competencyId = UUID.randomUUID();

        AnalyzedRecordSnapshot record =
                new AnalyzedRecordSnapshot(
                        recordId,
                        "API 성능 개선",
                        SNAPSHOT_AT.minusDays(1),
                        templateId,
                        "문제 해결 경험",
                        "API 응답 속도를 개선했습니다.",
                        List.of("쿼리 병목을 제거했습니다."),
                        List.of(
                                new StrengthTagSnapshot(
                                        tagId,
                                        "문제 해결",
                                        0.9f
                                )
                        )
                );

        InsightGenerationCommand command =
                commandWithCompetency(competencyId);

        when(analyzedRecordQuery.findAllForInsight(
                USER_ID,
                SNAPSHOT_AT
        )).thenReturn(List.of(record));

        when(strengthCalculator.calculate(List.of(record)))
                .thenReturn(List.of(
                        new StrengthStatistic(
                                tagId,
                                "문제 해결",
                                1,
                                0.9,
                                1.0,
                                1
                        )
                ));

        when(templateCalculator.calculate(List.of(record)))
                .thenReturn(List.of(
                        new TemplateStatistic(
                                templateId,
                                "문제 해결 경험",
                                1,
                                1.0,
                                1
                        )
                ));

        RecommendationCandidate candidate =
                new RecommendationCandidate(
                        recordId,
                        "API 성능 개선",
                        "문제 해결 경험",
                        "API 응답 속도를 개선했습니다.",
                        List.of("쿼리 병목을 제거했습니다."),
                        0.95
                );

        when(candidateQuery.findTopCandidates(
                USER_ID,
                competencyId,
                SNAPSHOT_AT,
                1,
                 0.0
        )).thenReturn(List.of(candidate));

        when(recommendationGenerator.generate(
                any(RecommendationRequest.class)
        )).thenReturn(new RecommendationResult(
                competencyId,
                recordId,
                "문제 해결 과정이 구체적으로 드러납니다."
        ));

        // when
        worker.generate(command, "lock-token");

        // then
        ArgumentCaptor<InsightGenerationResult> resultCaptor =
                ArgumentCaptor.forClass(
                        InsightGenerationResult.class
                );

        verify(resultWriter).complete(
                resultCaptor.capture()
        );

        verify(runningWriter).markRunning(INSIGHT_ID);

        InsightGenerationResult result =
                resultCaptor.getValue();

        assertThat(result.insightId()).isEqualTo(INSIGHT_ID);
        assertThat(result.strengths()).hasSize(1);
        assertThat(result.templates()).hasSize(1);
        assertThat(result.recommendations()).hasSize(1);

        assertThat(result.strengths().get(0).records())
                .extracting(
                        InsightGenerationResult
                                .StrengthRecordResult
                                ::recordId
                )
                .containsExactly(recordId);

        assertThat(result.recommendations().get(0).recordId())
                .isEqualTo(recordId);

        assertThat(result.recommendations().get(0).similarity())
                .isEqualTo(0.95);

        verify(failureWriter, never()).fail(
                any(),
                any(),
                any()
        );

        verify(generationLock).release(
                USER_ID,
                "lock-token"
        );
    }

    @Test
    void usesSnapshotAtWhenLoadingRecordsAndCandidates() {
        // given
        UUID competencyId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        InsightGenerationCommand command =
                commandWithCompetency(competencyId);

        AnalyzedRecordSnapshot record =
                new AnalyzedRecordSnapshot(
                        recordId,
                        "스냅샷 기록",
                        SNAPSHOT_AT.minusMinutes(1),
                        UUID.randomUUID(),
                        "기본 템플릿",
                        "요약",
                        List.of("근거"),
                        List.of()
                );

        when(analyzedRecordQuery.findAllForInsight(
                USER_ID,
                SNAPSHOT_AT
        )).thenReturn(List.of(record));

        when(strengthCalculator.calculate(any()))
                .thenReturn(List.of());

        when(templateCalculator.calculate(any()))
                .thenReturn(List.of());

        RecommendationCandidate candidate =
                new RecommendationCandidate(
                        recordId,
                        "스냅샷 기록",
                        "기본 템플릿",
                        "요약",
                        List.of("근거"),
                        0.8
                );

        when(candidateQuery.findTopCandidates(
                USER_ID,
                competencyId,
                SNAPSHOT_AT,
                1,
                 0.0
        )).thenReturn(List.of(candidate));

        when(recommendationGenerator.generate(any()))
                .thenReturn(new RecommendationResult(
                        competencyId,
                        recordId,
                        "추천 이유"
                ));

        // when
        worker.generate(command, "token");

        // then
        verify(analyzedRecordQuery).findAllForInsight(
                USER_ID,
                SNAPSHOT_AT
        );

        verify(candidateQuery).findTopCandidates(
                USER_ID,
                competencyId,
                SNAPSHOT_AT,
                1,
                 0.0
        );
    }

    @Test
    void generatesRecommendationForAllFiveCompetencies() {
        // given
        UUID recordId = UUID.randomUUID();

        List<JobCompetencySnapshot> competencies =
                IntStream.range(0, 5)
                        .mapToObj(index ->
                                new JobCompetencySnapshot(
                                        UUID.randomUUID(),
                                        "competency-" + index,
                                        "description-" + index
                                )
                        )
                        .toList();

        InsightGenerationCommand command =
                new InsightGenerationCommand(
                        INSIGHT_ID,
                        USER_ID,
                        JOB_ID,
                        "backend-developer",
                        SNAPSHOT_AT,
                        competencies
                );

        AnalyzedRecordSnapshot record =
                new AnalyzedRecordSnapshot(
                        recordId,
                        "record-title",
                        SNAPSHOT_AT.minusDays(1),
                        UUID.randomUUID(),
                        "template-name",
                        "summary",
                        List.of("evidence"),
                        List.of()
                );

        RecommendationCandidate candidate =
                new RecommendationCandidate(
                        recordId,
                        record.recordTitle(),
                        record.templateName(),
                        record.summary(),
                        record.evidenceSnippets(),
                        0.9
                );

        when(analyzedRecordQuery.findAllForInsight(
                USER_ID,
                SNAPSHOT_AT
        )).thenReturn(List.of(record));

        when(strengthCalculator.calculate(any()))
                .thenReturn(List.of());

        when(templateCalculator.calculate(any()))
                .thenReturn(List.of());

        when(candidateQuery.findTopCandidates(
                eq(USER_ID),
                any(UUID.class),
                eq(SNAPSHOT_AT),
                 eq(1),
                 eq(0.0)
        )).thenReturn(List.of(candidate));

        when(recommendationGenerator.generate(
                any(RecommendationRequest.class)
        )).thenAnswer(invocation -> {
            RecommendationRequest request =
                    invocation.getArgument(0);

            return new RecommendationResult(
                    request.jobCompetencyId(),
                    recordId,
                    "recommendation-reason"
            );
        });

        // when
        worker.generate(command, "lock-token");

        // then
        verify(candidateQuery, times(5))
                .findTopCandidates(
                        eq(USER_ID),
                        any(UUID.class),
                        eq(SNAPSHOT_AT),
                         eq(1),
                         eq(0.0)
                );

        verify(recommendationGenerator, times(5))
                .generate(any(RecommendationRequest.class));

        ArgumentCaptor<InsightGenerationResult> resultCaptor =
                ArgumentCaptor.forClass(
                        InsightGenerationResult.class
                );

        verify(resultWriter).complete(
                resultCaptor.capture()
        );

        assertThat(resultCaptor.getValue().recommendations())
                .hasSize(5)
                .extracting(
                        InsightGenerationResult
                                .JobRecommendationResult
                                ::jobCompetencyId
                )
                .containsExactlyElementsOf(
                        competencies.stream()
                                .map(JobCompetencySnapshot::jobCompetencyId)
                                .toList()
                );

        verify(failureWriter, never()).fail(
                any(),
                any(),
                any()
        );

        verify(generationLock).release(
                USER_ID,
                "lock-token"
        );
    }

    @Test
    void marksInsightAsFailedWhenRecordsAreEmpty() {
        // given
        InsightGenerationCommand command =
                commandWithCompetency(UUID.randomUUID());

        when(analyzedRecordQuery.findAllForInsight(
                USER_ID,
                SNAPSHOT_AT
        )).thenReturn(List.of());

        // when
        worker.generate(command, "lock-token");

        // then
        verify(resultWriter, never()).complete(any());

        verify(failureWriter).fail(
                eq(INSIGHT_ID),
                eq(InsightErrorCode.INSIGHT_ANALYZED_RECORDS_EMPTY.getCode()),
                eq(InsightErrorCode.INSIGHT_ANALYZED_RECORDS_EMPTY.getMessage())
        );

        verify(generationLock).release(
                USER_ID,
                "lock-token"
        );
    }

    @Test
    void storesNoMatchWithoutCallingAiWhenCandidatesAreEmpty() {
        // given
        UUID competencyId = UUID.randomUUID();

        InsightGenerationCommand command =
                commandWithCompetency(competencyId);

        AnalyzedRecordSnapshot record =
                new AnalyzedRecordSnapshot(
                        UUID.randomUUID(),
                        "기록",
                        SNAPSHOT_AT.minusDays(1),
                        UUID.randomUUID(),
                        "템플릿",
                        "요약",
                        List.of(),
                        List.of()
                );

        when(analyzedRecordQuery.findAllForInsight(
                USER_ID,
                SNAPSHOT_AT
        )).thenReturn(List.of(record));

        when(strengthCalculator.calculate(any()))
                .thenReturn(List.of());

        when(templateCalculator.calculate(any()))
                .thenReturn(List.of());

        when(candidateQuery.findTopCandidates(
                USER_ID,
                competencyId,
                SNAPSHOT_AT,
                1,
                 0.0
        )).thenReturn(List.of());

        // when
        worker.generate(command, "token");

        // then
        ArgumentCaptor<InsightGenerationResult> resultCaptor =
                ArgumentCaptor.forClass(InsightGenerationResult.class);
        verify(resultWriter).complete(resultCaptor.capture());
        assertThat(resultCaptor.getValue().recommendations())
                .singleElement()
                .satisfies(recommendation -> {
                    assertThat(recommendation.matched()).isFalse();
                    assertThat(recommendation.recordId()).isNull();
                });

        verify(failureWriter, never()).fail(any(), any(), any());

        verify(recommendationGenerator, never())
                .generate(any());

        verify(generationLock).release(
                USER_ID,
                "token"
        );
    }

    @Test
    void recordsAiFailureWhenRecommendationRetriesAreExhausted() {
        // given: 분석과 후보 조회까지는 성공했지만 AI 추천이 최종 실패한 상황
        UUID competencyId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        InsightGenerationCommand command =
                commandWithCompetency(competencyId);

        AnalyzedRecordSnapshot record = new AnalyzedRecordSnapshot(
                recordId,
                "추천 대상 기록",
                SNAPSHOT_AT.minusDays(1),
                UUID.randomUUID(),
                "경험 템플릿",
                "분석 요약",
                List.of("근거 문장"),
                List.of()
        );
        RecommendationCandidate candidate =
                new RecommendationCandidate(
                        recordId,
                        "추천 대상 기록",
                        "경험 템플릿",
                        "분석 요약",
                        List.of("근거 문장"),
                        0.91
                );

        when(analyzedRecordQuery.findAllForInsight(
                USER_ID,
                SNAPSHOT_AT
        )).thenReturn(List.of(record));
        when(strengthCalculator.calculate(List.of(record)))
                .thenReturn(List.of());
        when(templateCalculator.calculate(List.of(record)))
                .thenReturn(List.of());
        when(candidateQuery.findTopCandidates(
                USER_ID,
                competencyId,
                SNAPSHOT_AT,
                1,
                 0.0
        )).thenReturn(List.of(candidate));
        when(recommendationGenerator.generate(any()))
                .thenThrow(new CustomException(
                        InsightErrorCode
                                .INSIGHT_RECOMMENDATION_AI_SERVICE_FAILED
                ));

        // when
        worker.generate(command, "lock-token");

        // then: 부분 결과를 완료 처리하지 않고 실패 코드와 함께 FAILED 저장
        verify(resultWriter, never()).complete(any());
        verify(failureWriter).fail(
                INSIGHT_ID,
                InsightErrorCode
                        .INSIGHT_RECOMMENDATION_AI_SERVICE_FAILED
                        .getCode(),
                InsightErrorCode
                        .INSIGHT_RECOMMENDATION_AI_SERVICE_FAILED
                        .getMessage()
        );
        verify(generationLock).release(USER_ID, "lock-token");
    }

    @Test
    void marksInsightAsFailedWhenUnexpectedExceptionOccurs() {
        // given
        InsightGenerationCommand command =
                commandWithCompetency(UUID.randomUUID());

        when(analyzedRecordQuery.findAllForInsight(
                USER_ID,
                SNAPSHOT_AT
        )).thenThrow(
                new IllegalStateException("database failure")
        );

        // when
        worker.generate(command, "token");

        // then
        verify(failureWriter).fail(
                eq(INSIGHT_ID),
                eq(InsightErrorCode.INSIGHT_GENERATION_FAILED.getCode()),
                any()
        );

        verify(generationLock).release(
                USER_ID,
                "token"
        );
    }


    private InsightGenerationCommand commandWithCompetency(
            UUID competencyId
    ) {
        return new InsightGenerationCommand(
                INSIGHT_ID,
                USER_ID,
                JOB_ID,
                "백엔드 개발자",
                SNAPSHOT_AT,
                List.of(
                        new JobCompetencySnapshot(
                                competencyId,
                                "문제 해결",
                                "복잡한 문제를 분석하고 해결하는 역량"
                        )
                )
        );
    }
}
