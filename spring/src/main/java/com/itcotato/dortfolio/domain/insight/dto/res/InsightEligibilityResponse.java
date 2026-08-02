package com.itcotato.dortfolio.domain.insight.dto.res;

import java.time.LocalDateTime;

public record InsightEligibilityResponse(
        boolean eligible,
        InsightEligibilityReason reason,
        LocalDateTime nextAvailableAt,
        long completedRecordCount,
        int requiredRecordCount
) {
}
