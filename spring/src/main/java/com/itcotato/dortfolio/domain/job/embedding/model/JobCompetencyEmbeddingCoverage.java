package com.itcotato.dortfolio.domain.job.embedding.model;

import java.util.List;
import java.util.UUID;

public record JobCompetencyEmbeddingCoverage(
        String embeddingModel,
        long totalCount,
        long embeddedCount,
        List<UUID> missingJobCompetencyIds
) {

    public JobCompetencyEmbeddingCoverage {
        missingJobCompetencyIds = List.copyOf(missingJobCompetencyIds);
    }

    public long missingCount() {
        return missingJobCompetencyIds.size();
    }

    public boolean isComplete() {
        return missingJobCompetencyIds.isEmpty()
                && totalCount == embeddedCount;
    }
}
