package com.itcotato.dortfolio.domain.insight.dto.res;

import java.time.LocalDateTime;

public record InsightEligibilityResponse(
        boolean eligible,
        InsightEligibilityReason reason,
        LocalDateTime nextAvailableAt,
        long completedRecordCount,
        long analyzedRecordCount,
        int requiredRecordCount,
        long totalRecordCount,
        long analysisCompletedCount,
        long analysisFailedCount,
        long analysisInProgressCount
) {
    public InsightEligibilityResponse(
            boolean eligible,
            InsightEligibilityReason reason,
            LocalDateTime nextAvailableAt,
            long completedRecordCount,
            long analyzedRecordCount,
            int requiredRecordCount
    ) {
        this(
                eligible,
                reason,
                nextAvailableAt,
                completedRecordCount,
                analyzedRecordCount,
                requiredRecordCount,
                completedRecordCount,
                0,
                0,
                0
        );
    }
}
