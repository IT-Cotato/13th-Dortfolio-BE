package com.itcotato.dortfolio.domain.insight.dto.res;

import java.time.LocalDateTime;

public record InsightEligibilityResponse(
        boolean eligible,
        InsightEligibilityReason reaseon,
        LocalDateTime nextAvailableAt,
        long completedRecordCount,
        int requiredRecordCount
) {
}
