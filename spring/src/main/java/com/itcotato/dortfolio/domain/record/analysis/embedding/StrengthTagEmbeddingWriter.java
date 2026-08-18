package com.itcotato.dortfolio.domain.record.analysis.embedding;

import java.util.UUID;

public interface StrengthTagEmbeddingWriter {

	boolean saveIfAbsent(
		UUID strengthTagId,
		String embeddingModel,
		float[] embedding
	);
}
