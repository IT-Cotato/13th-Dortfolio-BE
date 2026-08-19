package com.itcotato.dortfolio.domain.insight.recommendation.service;

import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationRequest;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationResult;
import java.util.List;

public interface InsightRecommendationGenerator {

    List<RecommendationResult> generate(
            RecommendationRequest request
    );
}
