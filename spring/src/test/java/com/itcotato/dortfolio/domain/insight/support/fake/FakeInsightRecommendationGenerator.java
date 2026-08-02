package com.itcotato.dortfolio.domain.insight.support.fake;

import com.itcotato.dortfolio.domain.insight.recommendation.InsightRecommendationGenerator;
import com.itcotato.dortfolio.domain.insight.recommendation.RecommendationRequest;
import com.itcotato.dortfolio.domain.insight.recommendation.RecommendationResult;

import java.util.Objects;

public class FakeInsightRecommendationGenerator implements InsightRecommendationGenerator {

    private RecommendationResult result;
    private RecommendationRequest requestedRequest;

    public void setResult(RecommendationResult result) {
        this.result = result;
    }

    public RecommendationRequest getRequestedRequest() {
        return requestedRequest;
    }

    @Override
    public RecommendationResult generate(
            RecommendationRequest request
    ) {
        this.requestedRequest = request;

        return Objects.requireNonNull(
                result,
                "Insight recommendation result is not configured."
        );
    }
}
