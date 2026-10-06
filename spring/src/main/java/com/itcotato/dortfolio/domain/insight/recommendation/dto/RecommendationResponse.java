package com.itcotato.dortfolio.domain.insight.recommendation.dto;

import com.itcotato.dortfolio.global.ai.generation.GenerationMetadata;
import java.util.List;

public record RecommendationResponse(
        List<RecommendationResult> recommendations,
        GenerationMetadata metadata
) {
    public static RecommendationResponse of(List<RecommendationResult> recommendations, GenerationMetadata metadata) {
        return new RecommendationResponse(recommendations, metadata);
    }
}
