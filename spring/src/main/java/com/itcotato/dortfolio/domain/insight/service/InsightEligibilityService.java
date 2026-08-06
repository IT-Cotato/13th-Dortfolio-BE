package com.itcotato.dortfolio.domain.insight.service;

import com.itcotato.dortfolio.domain.insight.config.InsightProperties;
import com.itcotato.dortfolio.domain.insight.dto.res.InsightEligibilityReason;
import com.itcotato.dortfolio.domain.insight.dto.res.InsightEligibilityResponse;
import com.itcotato.dortfolio.domain.insight.entity.Insight;
import com.itcotato.dortfolio.domain.insight.entity.InsightGenerationStatus;
import com.itcotato.dortfolio.domain.insight.repository.InsightRecordQueryRepository;
import com.itcotato.dortfolio.domain.insight.repository.InsightRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordRepository;
import com.itcotato.dortfolio.domain.user.entity.UserJob;
import com.itcotato.dortfolio.domain.user.repository.UserJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InsightEligibilityService implements InsightEligibilityChecker {

    private final UserJobRepository userJobRepository;
    private final RecordRepository recordRepository;
    private final InsightRecordQueryRepository insightRecordQueryRepository;
    private final InsightRepository insightRepository;
    private final InsightProperties properties;
    private final Clock insightClock;

    @Override
    public InsightEligibilityResponse check(UUID userId) {
        int requiredCount = properties.minimumAnalyzedRecordCount();

        UserJob primaryUserJob = userJobRepository
                .findByUserIdAndIsPrimaryTrue(userId)
                .orElse(null);

        if (primaryUserJob == null) {
            return unavailable(
                    InsightEligibilityReason.JOB_NOT_CONFIGURED,
                    null,
                    0,
                    0,
                    requiredCount
            );
        }

        long completedRecordCount =
                recordRepository.countAvailableCompletedByUserId(userId);

        if (completedRecordCount < requiredCount) {
            return unavailable(
                    InsightEligibilityReason.NOT_ENOUGH_COMPLETED_RECORDS,
                    null,
                    completedRecordCount,
                    0,
                    requiredCount
            );
        }

        long analyzedRecordCount =
                insightRecordQueryRepository.countEligibleRecords(userId);

        if (analyzedRecordCount < requiredCount) {
            return unavailable(
                    InsightEligibilityReason.NOT_ENOUGH_ANALYZED_RECORDS,
                    null,
                    completedRecordCount,
                    analyzedRecordCount,
                    requiredCount
            );
        }

        boolean generationInProgress =
                insightRepository.existsByUser_IdAndStatus(
                        userId,
                        InsightGenerationStatus.PENDING
                );

        if (generationInProgress) {
            return unavailable(
                    InsightEligibilityReason.GENERATION_IN_PROGRESS,
                    null,
                    completedRecordCount,
                    analyzedRecordCount,
                    requiredCount
            );
        }

        Insight latestCompletedInsight = insightRepository
                .findLatestCompletedByUserId(userId)
                .orElse(null);

        // 최초 생성
        if (latestCompletedInsight == null) {
            return available(
                    completedRecordCount,
                    analyzedRecordCount,
                    requiredCount
            );
        }

        LocalDateTime now = LocalDateTime.now(insightClock);
        LocalDateTime nextAvailableAt =
                latestCompletedInsight.getCompletedAt()
                        .plus(properties.regenerationCooldown());

        // 정확히 24시간이 된 시점은 허용
        if (now.isBefore(nextAvailableAt)) {
            return unavailable(
                    InsightEligibilityReason.COOLDOWN,
                    nextAvailableAt,
                    completedRecordCount,
                    analyzedRecordCount,
                    requiredCount
            );
        }

        UUID currentJobId = primaryUserJob.getJob().getId();

        boolean jobChanged =
                !currentJobId.equals(
                        latestCompletedInsight.getJobIdSnapshot()
                );

        boolean hasNewAnalyzedRecord =
                insightRecordQueryRepository
                        .existsEligibleRecordAnalyzedAfter(
                                userId,
                                latestCompletedInsight.getRecordSnapshotAt()
                        );

        if (!jobChanged && !hasNewAnalyzedRecord) {
            return unavailable(
                    InsightEligibilityReason.NO_CHANGES,
                    null,
                    completedRecordCount,
                    analyzedRecordCount,
                    requiredCount
            );
        }

        return available(
                completedRecordCount,
                analyzedRecordCount,
                requiredCount
        );
    }

    private InsightEligibilityResponse available(
            long completedRecordCount,
            long analyzedRecordCount,
            int requiredRecordCount
    ) {
        return new InsightEligibilityResponse(
                true,
                InsightEligibilityReason.AVAILABLE,
                null,
                completedRecordCount,
                analyzedRecordCount,
                requiredRecordCount
        );
    }

    private InsightEligibilityResponse unavailable(
            InsightEligibilityReason reason,
            LocalDateTime nextAvailableAt,
            long completedRecordCount,
            long analyzedRecordCount,
            int requiredRecordCount
    ) {
        return new InsightEligibilityResponse(
                false,
                reason,
                nextAvailableAt,
                completedRecordCount,
                analyzedRecordCount,
                requiredRecordCount
        );
    }
}
