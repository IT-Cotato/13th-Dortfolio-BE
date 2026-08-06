package com.itcotato.dortfolio.domain.insight.recommendation.service;

import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationRequest;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationResult;

public interface InsightRecommendationGenerator {

    RecommendationResult generate(
            RecommendationRequest request
    );
}
