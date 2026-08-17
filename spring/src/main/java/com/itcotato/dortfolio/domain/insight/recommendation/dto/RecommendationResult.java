package com.itcotato.dortfolio.domain.insight.recommendation.dto;

import java.util.UUID;

public record RecommendationResult(
        boolean matched,
        UUID jobCompetencyId,
        UUID recordId,
        String reason
) {
    public RecommendationResult(
            UUID jobCompetencyId,
            UUID recordId,
            String reason
    ) {
        this(true, jobCompetencyId, recordId, reason);
    }
}
