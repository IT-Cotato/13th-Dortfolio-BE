package com.itcotato.dortfolio.domain.insight.support.fake;

import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationRequest;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationResponse;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationResult;
import com.itcotato.dortfolio.domain.insight.recommendation.service.InsightRecommendationGenerator;

import java.util.List;
import java.util.Objects;

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
    public RecommendationResponse generate(
            RecommendationRequest request
    ) {
        this.requestedRequest = request;

        return RecommendationResponse.of(Objects.requireNonNull(
                result,
                "Insight recommendation result is not configured."
        ), null);
    }
}
