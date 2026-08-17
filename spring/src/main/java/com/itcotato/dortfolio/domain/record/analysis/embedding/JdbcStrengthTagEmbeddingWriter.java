package com.itcotato.dortfolio.domain.record.analysis.embedding;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JdbcStrengthTagEmbeddingWriter implements StrengthTagEmbeddingWriter {

	private final JdbcTemplate jdbcTemplate;

	@Override
	public boolean saveIfAbsent(
		UUID strengthTagId,
		String embeddingModel,
		float[] embedding
	) {
		int inserted = jdbcTemplate.update("""
				insert into strength_tag_embeddings (
					id,
					created_at,
					updated_at,
					strength_tag_id,
					embedding_model,
					embedding
				) values (?, current_timestamp, current_timestamp, ?, ?, cast(? as vector))
				on conflict (strength_tag_id, embedding_model) do nothing
				""",
			UUID.randomUUID(),
			strengthTagId,
			embeddingModel,
			toVectorLiteral(embedding)
		);

		return inserted == 1;
	}

	private String toVectorLiteral(float[] embedding) {
		StringBuilder builder = new StringBuilder("[");
		for (int index = 0; index < embedding.length; index++) {
			if (index > 0) {
				builder.append(',');
			}
			builder.append(Float.toString(embedding[index]));
		}
		return builder.append(']').toString();
	}
}
