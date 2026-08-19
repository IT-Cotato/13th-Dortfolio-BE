package com.itcotato.dortfolio.domain.insight.recommendation.dto;

import java.util.List;

public record RecommendationResponse(
        List<RecommendationResult> recommendations
) {
}
