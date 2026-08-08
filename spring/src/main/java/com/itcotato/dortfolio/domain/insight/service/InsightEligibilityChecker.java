package com.itcotato.dortfolio.domain.insight.service;

import com.itcotato.dortfolio.domain.insight.dto.res.InsightEligibilityResponse;

import java.util.UUID;
import java.time.LocalDateTime;

public interface InsightEligibilityChecker {

    InsightEligibilityResponse check(UUID userId);

    default InsightEligibilityResponse check(
            UUID userId,
            LocalDateTime snapshotAt
    ) {
        return check(userId);
    }
}
