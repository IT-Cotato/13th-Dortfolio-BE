package com.itcotato.dortfolio.domain.insight.recommendation.client;

import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationRequest;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationResult;

public interface InsightRecommendationClient {

    RecommendationResult generate(
            RecommendationRequest request
    );
}
