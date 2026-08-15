package com.itcotato.dortfolio.domain.job.embedding.model;

import java.util.List;

public record JobCompetencyEmbeddingBatchResult(
        long totalCount,
        long generatedCount,
        long skippedCount,
        long failedCount,
        List<JobCompetencyEmbeddingFailure> failures
) {

    public JobCompetencyEmbeddingBatchResult {
        failures = List.copyOf(failures);
    }

    public boolean hasFailures() {
        return failedCount > 0;
    }
}
