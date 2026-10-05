package com.itcotato.dortfolio.domain.insight.recommendation.client;

import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationRequest;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationResponse;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationResult;
import java.util.List;
import java.util.UUID;

public interface InsightRecommendationClient {

    List<RecommendationResult> generate(RecommendationRequest request);

    default RecommendationResponse generateWithUsage(UUID requestId, RecommendationRequest request) {
        return new RecommendationResponse(generate(request), null);
    }
}
