package com.itcotato.dortfolio.domain.insight.recommendation.client;

import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationRequest;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationResponse;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationResult;
import java.util.List;
import java.util.UUID;

public interface InsightRecommendationClient {

    RecommendationResponse generate(RecommendationRequest request);

    default RecommendationResponse generateWithUsage(UUID requestId, RecommendationRequest request) {
        return generate(request);
    }
}
