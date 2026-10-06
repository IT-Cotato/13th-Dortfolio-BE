package com.itcotato.dortfolio.domain.insight.recommendation.service;

import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationRequest;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationResponse;

public interface InsightRecommendationGenerator {

    RecommendationResponse generate(
            RecommendationRequest request
    );
}
