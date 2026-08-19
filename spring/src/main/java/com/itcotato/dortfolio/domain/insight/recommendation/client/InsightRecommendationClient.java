package com.itcotato.dortfolio.domain.insight.recommendation.client;

import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationRequest;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationResult;
import java.util.List;

public interface InsightRecommendationClient {

    List<RecommendationResult> generate(
            RecommendationRequest request
    );
}
