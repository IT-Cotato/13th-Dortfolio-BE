package com.itcotato.dortfolio.domain.insight.recommendation.client;

import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationRequest;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationResponse;

public interface InsightRecommendationClient {

    RecommendationResponse generate(
            RecommendationRequest request
    );
}
