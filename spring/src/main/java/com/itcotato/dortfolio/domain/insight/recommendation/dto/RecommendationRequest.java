package com.itcotato.dortfolio.domain.insight.recommendation.dto;

import java.util.List;
import java.util.UUID;

public record RecommendationRequest(
        UUID jobId,
        String jobName,
        List<RecommendationCompetency> competencies
) {
}
