package com.itcotato.dortfolio.domain.insight.support.fake;

import com.itcotato.dortfolio.domain.insight.recommendation.service.InsightRecommendationGenerator;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationRequest;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationResult;

import java.util.Objects;
import java.util.List;

public class FakeInsightRecommendationGenerator implements InsightRecommendationGenerator {

    private List<RecommendationResult> result;
    private RecommendationRequest requestedRequest;

    public void setResult(RecommendationResult result) {
        this.result = List.of(result);
    }

    public RecommendationRequest getRequestedRequest() {
        return requestedRequest;
    }

    @Override
    public List<RecommendationResult> generate(
            RecommendationRequest request
    ) {
        this.requestedRequest = request;

        return Objects.requireNonNull(
                result,
                "Insight recommendation result is not configured."
        );
    }
}
