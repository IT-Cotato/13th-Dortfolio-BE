package com.itcotato.dortfolio.domain.record.analysis.embedding;

import java.util.List;
import java.util.UUID;

public record StrengthTagEmbeddingCoverage(
	String embeddingModel,
	long totalCount,
	long embeddedCount,
	List<UUID> missingStrengthTagIds
) {

	public StrengthTagEmbeddingCoverage {
		missingStrengthTagIds = List.copyOf(missingStrengthTagIds);
	}

	public long missingCount() {
		return missingStrengthTagIds.size();
	}

	public boolean isComplete() {
		return missingStrengthTagIds.isEmpty() && totalCount == embeddedCount;
	}
}
