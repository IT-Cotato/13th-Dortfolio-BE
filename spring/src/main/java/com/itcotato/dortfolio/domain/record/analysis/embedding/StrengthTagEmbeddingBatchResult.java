package com.itcotato.dortfolio.domain.record.analysis.embedding;

import java.util.List;

public record StrengthTagEmbeddingBatchResult(
	long totalCount,
	long generatedCount,
	long skippedCount,
	long failedCount,
	List<StrengthTagEmbeddingFailure> failures
) {

	public StrengthTagEmbeddingBatchResult {
		failures = List.copyOf(failures);
	}

	public boolean hasFailures() {
		return failedCount > 0;
	}
}
