package com.itcotato.dortfolio.domain.insight.recommendation.dto;

import com.itcotato.dortfolio.global.ai.observability.dto.AiUsageResponse;

import com.itcotato.dortfolio.global.ai.generation.GenerationMetadata;
import java.util.List;

public record RecommendationResponse(
        List<RecommendationResult> recommendations,
        AiUsageResponse usage,
        GenerationMetadata metadata
) {
    public RecommendationResponse(List<RecommendationResult> recommendations, AiUsageResponse usage) {
        this(recommendations, usage, null);
    }

    public static RecommendationResponse of(List<RecommendationResult> recommendations, GenerationMetadata metadata) {
        return new RecommendationResponse(recommendations, null, metadata);
    }
}
