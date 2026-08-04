package com.itcotato.dortfolio.domain.insight.recommendation.dto;

import java.util.List;
import java.util.UUID;

public record RecommendationRequest(
        UUID jobId,
        String jobName,
        UUID jobCompetencyId,
        String competencyName,
        String competencyDescription,
        List<RecommendationCandidate> candidates
) {
}
