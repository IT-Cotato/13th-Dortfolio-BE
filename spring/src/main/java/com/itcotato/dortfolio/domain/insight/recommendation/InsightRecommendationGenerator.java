package com.itcotato.dortfolio.domain.insight.recommendation;

public interface InsightRecommendationGenerator {

    RecommendationResult generate(
            RecommendationRequest request
    );
}
