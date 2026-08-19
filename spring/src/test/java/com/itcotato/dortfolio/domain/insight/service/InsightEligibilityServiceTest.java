package com.itcotato.dortfolio.domain.insight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.itcotato.dortfolio.domain.insight.config.InsightProperties;
import com.itcotato.dortfolio.domain.insight.dto.res.InsightEligibilityReason;
import com.itcotato.dortfolio.domain.insight.dto.res.InsightEligibilityResponse;
import com.itcotato.dortfolio.domain.insight.entity.Insight;
import com.itcotato.dortfolio.domain.insight.entity.InsightGenerationStatus;
import com.itcotato.dortfolio.domain.insight.repository.InsightRecordQueryRepository;
import com.itcotato.dortfolio.domain.insight.repository.InsightRepository;
import com.itcotato.dortfolio.domain.job.entity.Job;
import com.itcotato.dortfolio.domain.record.repository.RecordRepository;
import com.itcotato.dortfolio.domain.user.entity.UserJob;
import com.itcotato.dortfolio.domain.user.repository.UserJobRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InsightEligibilityServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CURRENT_JOB_ID = UUID.randomUUID();
    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 8, 2, 10, 0);
    private static final LocalDateTime SNAPSHOT_AT =
            LocalDateTime.of(2026, 8, 1, 9, 0);

    @Mock
    private UserJobRepository userJobRepository;

    @Mock
    private RecordRepository recordRepository;

    @Mock
    private InsightRecordQueryRepository insightRecordQueryRepository;

    @Mock
    private InsightRepository insightRepository;

    @Mock
    private UserJob primaryUserJob;

    @Mock
    private Job currentJob;

    @Mock
    private Insight latestCompletedInsight;

    private InsightEligibilityService eligibilityService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                NOW.atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
        );
        eligibilityService = new InsightEligibilityService(
                userJobRepository,
                recordRepository,
                insightRecordQueryRepository,
                insightRepository,
                new InsightProperties(
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
                ),
                clock
        );

        given(primaryUserJob.getJob()).willReturn(currentJob);
        given(currentJob.getId()).willReturn(CURRENT_JOB_ID);
    }

    @Test
    void rejectsWhenPrimaryJobIsNotConfigured() {
        given(userJobRepository.findByUserIdAndIsPrimaryTrue(USER_ID))
                .willReturn(Optional.empty());

        InsightEligibilityResponse result = eligibilityService.check(USER_ID);

        assertThat(result.eligible()).isFalse();
        assertThat(result.reason())
                .isEqualTo(InsightEligibilityReason.JOB_NOT_CONFIGURED);
    }

    @Test
    void rejectsNineCompletedRecords() {
        givenPrimaryJob();
        given(recordRepository.countAvailableCompletedByUserId(USER_ID))
                .willReturn(9L);
        given(insightRecordQueryRepository.countEligibleRecords(USER_ID))
                .willReturn(9L);

        InsightEligibilityResponse result = eligibilityService.check(USER_ID);

        assertThat(result.reason())
                .isEqualTo(InsightEligibilityReason.NOT_ENOUGH_COMPLETED_RECORDS);
        assertThat(result.completedRecordCount()).isEqualTo(9);
        assertThat(result.analyzedRecordCount()).isEqualTo(9);
        assertThat(result.requiredRecordCount()).isEqualTo(10);
    }

    @Test
    void rejectsNineAnalyzedRecordsEvenWhenTenRecordsAreCompleted() {
        givenPrimaryJob();
        given(recordRepository.countAvailableCompletedByUserId(USER_ID))
                .willReturn(10L);
        given(insightRecordQueryRepository.countEligibleRecords(USER_ID))
                .willReturn(9L);

        InsightEligibilityResponse result = eligibilityService.check(USER_ID);

        assertThat(result.reason())
                .isEqualTo(InsightEligibilityReason.NOT_ENOUGH_ANALYZED_RECORDS);
        assertThat(result.analyzedRecordCount()).isEqualTo(9);
    }

    @Test
    void allowsFirstGenerationWithExactlyTenAnalyzedRecords() {
        givenBaseEligibilityConditions();
        given(insightRepository.findLatestCompletedByUserId(USER_ID))
                .willReturn(Optional.empty());

        InsightEligibilityResponse result = eligibilityService.check(USER_ID);

        assertThat(result.eligible()).isTrue();
        assertThat(result.reason()).isEqualTo(InsightEligibilityReason.AVAILABLE);
        assertThat(result.completedRecordCount()).isEqualTo(10);
        assertThat(result.analyzedRecordCount()).isEqualTo(10);
    }

    @Test
    void checksGenerationEligibilityAtRequestedSnapshot() {
        givenPrimaryJob();
        given(recordRepository.countAvailableCompletedByUserIdAt(
                USER_ID,
                SNAPSHOT_AT
        )).willReturn(10L);
        given(insightRecordQueryRepository.countEligibleRecordsAt(
                USER_ID,
                SNAPSHOT_AT
        )).willReturn(10L);
        given(insightRepository.existsByUser_IdAndStatusIn(
                USER_ID,
                java.util.List.of(
                        InsightGenerationStatus.PENDING,
                        InsightGenerationStatus.RUNNING
                )
        )).willReturn(false);
        given(insightRepository.findLatestCompletedByUserId(USER_ID))
                .willReturn(Optional.empty());

        InsightEligibilityResponse result =
                eligibilityService.check(USER_ID, SNAPSHOT_AT);

        assertThat(result.eligible()).isTrue();
        assertThat(result.completedRecordCount()).isEqualTo(10);
        assertThat(result.analyzedRecordCount()).isEqualTo(10);
    }

    @Test
    void rejectsWhenGenerationIsInProgress() {
        givenBaseEligibilityConditions();
        given(insightRepository.existsByUser_IdAndStatus(
                USER_ID,
                InsightGenerationStatus.PENDING
        )).willReturn(true);

        InsightEligibilityResponse result = eligibilityService.check(USER_ID);

        assertThat(result.reason())
                .isEqualTo(InsightEligibilityReason.GENERATION_IN_PROGRESS);
    }

    @Test
    void rejectsImmediatelyBeforeCooldownEnds() {
        givenLatestCompletedInsight(NOW.minusHours(24).plusNanos(1));

        InsightEligibilityResponse result = eligibilityService.check(USER_ID);

        assertThat(result.reason()).isEqualTo(InsightEligibilityReason.COOLDOWN);
        assertThat(result.nextAvailableAt())
                .isEqualTo(NOW.plusNanos(1));
    }

    @Test
    void allowsAtExactTwentyFourHourBoundaryWhenNewRecordExists() {
        givenLatestCompletedInsight(NOW.minusHours(24));
        given(insightRecordQueryRepository.existsEligibleRecordAnalyzedAfter(
                USER_ID,
                SNAPSHOT_AT
        )).willReturn(true);

        InsightEligibilityResponse result = eligibilityService.check(USER_ID);

        assertThat(result.eligible()).isTrue();
        assertThat(result.reason()).isEqualTo(InsightEligibilityReason.AVAILABLE);
    }

    @Test
    void rejectsAfterCooldownWhenNothingHasChanged() {
        givenLatestCompletedInsight(NOW.minusHours(25));
        given(insightRecordQueryRepository.existsEligibleRecordAnalyzedAfter(
                USER_ID,
                SNAPSHOT_AT
        )).willReturn(false);

        InsightEligibilityResponse result = eligibilityService.check(USER_ID);

        assertThat(result.reason()).isEqualTo(InsightEligibilityReason.NO_CHANGES);
    }

    @Test
    void allowsAfterCooldownWhenJobHasChanged() {
        givenLatestCompletedInsight(NOW.minusHours(25));
        given(latestCompletedInsight.getJobIdSnapshot())
                .willReturn(UUID.randomUUID());

        InsightEligibilityResponse result = eligibilityService.check(USER_ID);

        assertThat(result.eligible()).isTrue();
        assertThat(result.reason()).isEqualTo(InsightEligibilityReason.AVAILABLE);
    }

    private void givenPrimaryJob() {
        given(userJobRepository.findByUserIdAndIsPrimaryTrue(USER_ID))
                .willReturn(Optional.of(primaryUserJob));
    }

    private void givenBaseEligibilityConditions() {
        givenPrimaryJob();
        given(recordRepository.countAvailableCompletedByUserId(USER_ID))
                .willReturn(10L);
        given(insightRecordQueryRepository.countEligibleRecords(USER_ID))
                .willReturn(10L);
        given(insightRepository.existsByUser_IdAndStatus(
                USER_ID,
                InsightGenerationStatus.PENDING
        )).willReturn(false);
    }

    private void givenLatestCompletedInsight(LocalDateTime completedAt) {
        givenBaseEligibilityConditions();
        given(insightRepository.findLatestCompletedByUserId(USER_ID))
                .willReturn(Optional.of(latestCompletedInsight));
        given(latestCompletedInsight.getCompletedAt()).willReturn(completedAt);
        given(latestCompletedInsight.getJobIdSnapshot()).willReturn(CURRENT_JOB_ID);
        given(latestCompletedInsight.getRecordSnapshotAt()).willReturn(SNAPSHOT_AT);
    }
}
