package com.itcotato.dortfolio.domain.insight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itcotato.dortfolio.domain.insight.dto.res.LatestInsightResponse;
import com.itcotato.dortfolio.domain.insight.entity.Insight;
import com.itcotato.dortfolio.domain.insight.entity.InsightGenerationStatus;
import com.itcotato.dortfolio.domain.insight.entity.InsightJobRecommendation;
import com.itcotato.dortfolio.domain.insight.entity.InsightStrength;
import com.itcotato.dortfolio.domain.insight.entity.InsightStrengthRecord;
import com.itcotato.dortfolio.domain.insight.entity.InsightTemplateStatistic;
import com.itcotato.dortfolio.domain.insight.repository.InsightJobRecommendationRepository;
import com.itcotato.dortfolio.domain.insight.repository.InsightRecordQueryRepository;
import com.itcotato.dortfolio.domain.insight.repository.InsightRepository;
import com.itcotato.dortfolio.domain.insight.repository.InsightStrengthRecordRepository;
import com.itcotato.dortfolio.domain.insight.repository.InsightStrengthRepository;
import com.itcotato.dortfolio.domain.insight.repository.InsightTemplateStatisticRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordRepository;
import com.itcotato.dortfolio.domain.record.analysis.entity.AiAnalysisStatus;
import com.itcotato.dortfolio.domain.job.entity.Job;
import com.itcotato.dortfolio.domain.user.entity.UserJob;
import com.itcotato.dortfolio.domain.user.repository.UserJobRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.InsightErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LatestInsightQueryServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID INSIGHT_ID = UUID.randomUUID();
    private static final UUID STRENGTH_ID = UUID.randomUUID();
    private static final UUID STRENGTH_TAG_ID = UUID.randomUUID();
    private static final UUID TEMPLATE_ID = UUID.randomUUID();
    private static final UUID COMPETENCY_ID = UUID.randomUUID();
    private static final UUID AVAILABLE_RECORD_ID = UUID.randomUUID();
    private static final UUID DELETED_RECORD_ID = UUID.randomUUID();
    private static final LocalDateTime SNAPSHOT_AT =
            LocalDateTime.of(2026, 8, 6, 16, 30);

    @Mock
    private InsightRepository insightRepository;

    @Mock
    private InsightStrengthRepository strengthRepository;

    @Mock
    private InsightStrengthRecordRepository strengthRecordRepository;

    @Mock
    private InsightTemplateStatisticRepository templateRepository;

    @Mock
    private InsightJobRecommendationRepository recommendationRepository;

    @Mock
    private RecordRepository recordRepository;

    @Mock
    private InsightRecordQueryRepository insightRecordQueryRepository;

    @Mock
    private UserJobRepository userJobRepository;

    @Mock
    private UserJob primaryUserJob;

    @Mock
    private Job currentJob;

    @Mock
    private Insight completedInsight;

    @Mock
    private Insight pendingInsight;

    @Mock
    private InsightStrength strength;

    @Mock
    private InsightStrengthRecord strengthRecord;

    @Mock
    private InsightTemplateStatistic template;

    @Mock
    private InsightJobRecommendation recommendation;

    private LatestInsightQueryService service;

    @BeforeEach
    void setUp() {
        service = new LatestInsightQueryService(
                insightRepository,
                strengthRepository,
                strengthRecordRepository,
                templateRepository,
                recommendationRepository,
                recordRepository,
                insightRecordQueryRepository,
                userJobRepository
        );
    }

    @Test
    void returnsLatestCompletedSnapshotAndCurrentPendingGeneration() {
        // given
        stubCompletedInsight();
        stubStrength();
        stubTemplate();
        stubRecommendation();

        when(insightRepository.findLatestCompletedByUserId(USER_ID))
                .thenReturn(Optional.of(completedInsight));
        when(strengthRepository
                .findAllByInsight_IdOrderByRankAscStrengthTagIdSnapshotAsc(
                        INSIGHT_ID
                )).thenReturn(List.of(strength));
        when(strengthRecordRepository.findAllForInsight(INSIGHT_ID))
                .thenReturn(List.of(strengthRecord));
        when(templateRepository
                .findAllByInsight_IdOrderByRankAscTemplateIdSnapshotAsc(
                        INSIGHT_ID
                )).thenReturn(List.of(template));
        when(recommendationRepository
                .findAllByInsight_IdOrderBySortOrderSnapshotAscJobCompetencyIdSnapshotAsc(
                        INSIGHT_ID
                )).thenReturn(List.of(recommendation));
        when(recordRepository.findAvailableRecordIds(
                USER_ID,
                Set.of(AVAILABLE_RECORD_ID, DELETED_RECORD_ID)
        )).thenReturn(Set.of(AVAILABLE_RECORD_ID));

        when(insightRepository.findPendingByUserId(USER_ID))
                .thenReturn(Optional.of(pendingInsight));
        when(pendingInsight.getId()).thenReturn(UUID.randomUUID());
        when(pendingInsight.getStatus())
                .thenReturn(InsightGenerationStatus.PENDING);
        when(pendingInsight.getRequestedAt())
                .thenReturn(SNAPSHOT_AT.plusHours(1));
        UUID currentJobId = UUID.randomUUID();
        when(userJobRepository.findByUserIdAndIsPrimaryTrue(USER_ID))
                .thenReturn(Optional.of(primaryUserJob));
        when(primaryUserJob.getJob()).thenReturn(currentJob);
        when(currentJob.getId()).thenReturn(currentJobId);
        when(insightRecordQueryRepository.countEligibleRecordsAnalyzedAfter(
                USER_ID,
                SNAPSHOT_AT
        )).thenReturn(3L);
        when(insightRecordQueryRepository.countRecordsByAnalysisStatus(
                USER_ID,
                AiAnalysisStatus.PENDING
        )).thenReturn(1L);
        when(insightRecordQueryRepository.countRecordsByAnalysisStatus(
                USER_ID,
                AiAnalysisStatus.FAILED
        )).thenReturn(2L);

        // when
        LatestInsightResponse result = service.getLatest(USER_ID);

        // then
        assertThat(result.insightId()).isEqualTo(INSIGHT_ID);
        assertThat(result.job().jobName())
                .isEqualTo("backend-developer");
        assertThat(result.analyzedRecordCount()).isEqualTo(12);

        assertThat(result.strengths()).hasSize(1);
        assertThat(result.strengths().get(0).rank()).isEqualTo(1);
        assertThat(result.strengths().get(0).records())
                .singleElement()
                .satisfies(record -> {
                    assertThat(record.recordId())
                            .isEqualTo(AVAILABLE_RECORD_ID);
                    assertThat(record.navigationAvailable()).isTrue();
                });

        assertThat(result.templates()).hasSize(1);
        assertThat(result.recommendations())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.recordId())
                            .isEqualTo(DELETED_RECORD_ID);
                    assertThat(item.navigationAvailable()).isFalse();
                });

        assertThat(result.currentGeneration()).isNotNull();
        assertThat(result.currentGeneration().status())
                .isEqualTo(InsightGenerationStatus.PENDING);
        assertThat(result.changes().newCompletedRecordCount())
                .isEqualTo(3);
        assertThat(result.changes().desiredJobChanged()).isTrue();
        assertThat(result.changes().analysisPendingRecordCount())
                .isEqualTo(1);
        assertThat(result.changes().analysisFailedRecordCount())
                .isEqualTo(2);
    }

    @Test
    void returnsEmptyResultsWithoutIssuingEmptyInQuery() {
        // given
        stubCompletedInsight();
        when(insightRepository.findLatestCompletedByUserId(USER_ID))
                .thenReturn(Optional.of(completedInsight));
        when(strengthRepository
                .findAllByInsight_IdOrderByRankAscStrengthTagIdSnapshotAsc(
                        INSIGHT_ID
                )).thenReturn(List.of());
        when(strengthRecordRepository.findAllForInsight(INSIGHT_ID))
                .thenReturn(List.of());
        when(templateRepository
                .findAllByInsight_IdOrderByRankAscTemplateIdSnapshotAsc(
                        INSIGHT_ID
                )).thenReturn(List.of());
        when(recommendationRepository
                .findAllByInsight_IdOrderBySortOrderSnapshotAscJobCompetencyIdSnapshotAsc(
                        INSIGHT_ID
                )).thenReturn(List.of());
        when(insightRepository.findPendingByUserId(USER_ID))
                .thenReturn(Optional.empty());
        when(userJobRepository.findByUserIdAndIsPrimaryTrue(USER_ID))
                .thenReturn(Optional.empty());

        // when
        LatestInsightResponse result = service.getLatest(USER_ID);

        // then
        assertThat(result.strengths()).isEmpty();
        assertThat(result.templates()).isEmpty();
        assertThat(result.recommendations()).isEmpty();
        assertThat(result.currentGeneration()).isNull();
        assertThat(result.changes().desiredJobChanged()).isTrue();
        verify(recordRepository, never())
                .findAvailableRecordIds(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()
                );
    }

    @Test
    void throwsNotFoundWhenCompletedInsightDoesNotExist() {
        // given
        when(insightRepository.findLatestCompletedByUserId(USER_ID))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.getLatest(USER_ID))
                .isInstanceOf(CustomException.class)
                .satisfies(exception ->
                        assertThat(((CustomException) exception)
                                .getErrorCode())
                                .isEqualTo(
                                        InsightErrorCode.INSIGHT_NOT_FOUND
                                )
                );
    }

    private void stubCompletedInsight() {
        when(completedInsight.getId()).thenReturn(INSIGHT_ID);
        when(completedInsight.getJobIdSnapshot())
                .thenReturn(UUID.randomUUID());
        when(completedInsight.getJobNameSnapshot())
                .thenReturn("backend-developer");
        when(completedInsight.getRecordSnapshotAt())
                .thenReturn(SNAPSHOT_AT);
        when(completedInsight.getCompletedAt())
                .thenReturn(SNAPSHOT_AT.plusSeconds(12));
        when(completedInsight.getBaseCompletedRecordCount())
                .thenReturn(12);
    }

    private void stubStrength() {
        when(strength.getId()).thenReturn(STRENGTH_ID);
        when(strength.getStrengthTagIdSnapshot())
                .thenReturn(STRENGTH_TAG_ID);
        when(strength.getStrengthNameSnapshot())
                .thenReturn("problem-solving");
        when(strength.getRecordCount()).thenReturn(1);
        when(strength.getAverageScore()).thenReturn(0.9);
        when(strength.getRatio()).thenReturn(1.0);
        when(strength.getRank()).thenReturn(1);

        when(strengthRecord.getInsightStrength())
                .thenReturn(strength);
        when(strengthRecord.getRecordIdSnapshot())
                .thenReturn(AVAILABLE_RECORD_ID);
        when(strengthRecord.getRecordTitleSnapshot())
                .thenReturn("record-title");
        when(strengthRecord.getRecordCompletedAtSnapshot())
                .thenReturn(SNAPSHOT_AT.minusDays(1));
    }

    private void stubTemplate() {
        when(template.getTemplateIdSnapshot()).thenReturn(TEMPLATE_ID);
        when(template.getTemplateNameSnapshot())
                .thenReturn("template-name");
        when(template.getRecordCount()).thenReturn(1);
        when(template.getRatio()).thenReturn(1.0);
        when(template.getRank()).thenReturn(1);
    }

    private void stubRecommendation() {
        when(recommendation.getJobCompetencyIdSnapshot())
                .thenReturn(COMPETENCY_ID);
        when(recommendation.getCompetencyNameSnapshot())
                .thenReturn("problem-solving");
        when(recommendation.getRecordIdSnapshot())
                .thenReturn(DELETED_RECORD_ID);
        when(recommendation.getRecordTitleSnapshot())
                .thenReturn("deleted-record-title");
        when(recommendation.getTemplateNameSnapshot())
                .thenReturn("template-name");
        when(recommendation.getReason())
                .thenReturn("recommendation-reason");
        when(recommendation.getSimilarity()).thenReturn(0.8);
    }
}
