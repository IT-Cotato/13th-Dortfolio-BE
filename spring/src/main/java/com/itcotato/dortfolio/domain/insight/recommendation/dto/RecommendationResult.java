package com.itcotato.dortfolio.domain.insight.recommendation.dto;

import java.util.UUID;

public record RecommendationResult(
        UUID jobCompetencyId,
        UUID recordId,
        String reason
) {
}
