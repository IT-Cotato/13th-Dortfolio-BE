package com.itcotato.dortfolio.domain.record.analysis.embedding;

import java.util.UUID;

public record StrengthTagEmbeddingFailure(
	UUID strengthTagId,
	String reason
) {
}
