package com.itcotato.dortfolio.domain.record.analysis.embedding;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "strength-tag-embedding.batch")
public record StrengthTagEmbeddingBatchProperties(
	boolean enabled,
	StrengthTagEmbeddingBatchCommand command
) {

	public StrengthTagEmbeddingBatchProperties {
		if (command == null) {
			command = StrengthTagEmbeddingBatchCommand.GENERATE_MISSING;
		}
	}
}
