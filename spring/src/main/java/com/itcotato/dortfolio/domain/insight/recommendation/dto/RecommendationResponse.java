package com.itcotato.dortfolio.domain.insight.recommendation.dto;

import com.itcotato.dortfolio.global.ai.observability.dto.AiUsageResponse;

import java.util.List;

public record RecommendationResponse(
        List<RecommendationResult> recommendations,
        AiUsageResponse usage
) {
}
