package com.itcotato.dortfolio.domain.insight.support.fake;

import com.itcotato.dortfolio.domain.insight.dto.res.InsightEligibilityResponse;
import com.itcotato.dortfolio.domain.insight.service.InsightEligibilityChecker;

import java.util.Objects;
import java.util.UUID;

public class FakeInsightEligibilityChecker implements InsightEligibilityChecker {

    private InsightEligibilityResponse response;
    private UUID requestedUserId;

    public void setResponse(InsightEligibilityResponse response) {
        this.response = response;
    }

    public UUID getRequestedUserId() {
        return requestedUserId;
    }

    @Override
    public InsightEligibilityResponse check(UUID userId) {
        this.requestedUserId = userId;

        return Objects.requireNonNull(
                response,
                "Insight eligibility response is not configured."
        );
    }
}
