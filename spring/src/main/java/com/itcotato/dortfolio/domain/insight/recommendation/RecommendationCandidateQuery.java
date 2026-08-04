package com.itcotato.dortfolio.domain.insight.recommendation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface RecommendationCandidateQuery {

    List<RecommendationCandidate> findTopCandidates(
            UUID userId,
            UUID jobCompetencyId,
            LocalDateTime snapshotAt,
            int limit
    );
}
