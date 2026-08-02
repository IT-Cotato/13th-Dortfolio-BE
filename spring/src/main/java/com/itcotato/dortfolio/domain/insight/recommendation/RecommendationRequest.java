package com.itcotato.dortfolio.domain.insight.recommendation;

import java.util.List;
import java.util.UUID;

public record RecommendationRequest(
        UUID jobCompetencyId,
        String jobName,
        String competencyName,
        String competencyDescription,
        List<RecommendationCandidate> candidates
) {
}
