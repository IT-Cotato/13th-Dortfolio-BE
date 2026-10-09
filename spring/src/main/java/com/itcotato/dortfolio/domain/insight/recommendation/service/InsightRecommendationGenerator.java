package com.itcotato.dortfolio.domain.insight.recommendation.service;

import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationRequest;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationResult;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationResponse;
import java.util.UUID;

public interface InsightRecommendationGenerator {

    RecommendationResponse generate(
            UUID userId,
            RecommendationRequest request
    );
}
