package com.itcotato.dortfolio.domain.insight.service;

import com.itcotato.dortfolio.domain.insight.dto.res.InsightEligibilityResponse;

import java.util.UUID;

public interface InsightEligibilityChecker {

    InsightEligibilityResponse check(UUID userId);
}
