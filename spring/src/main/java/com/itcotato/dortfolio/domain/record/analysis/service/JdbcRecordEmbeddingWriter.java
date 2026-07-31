package com.itcotato.dortfolio.domain.record.analysis.service;

import com.itcotato.dortfolio.domain.record.entity.Record;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JdbcRecordEmbeddingWriter implements RecordEmbeddingWriter {

	private final JdbcTemplate jdbcTemplate;

	@Override
	public void save(Record record, String embeddingModel, float[] embedding) {
		jdbcTemplate.update("""
				insert into record_embeddings
					(id, created_at, updated_at, record_id, embedding_model, embedding)
				values
					(?, current_timestamp, current_timestamp, ?, ?, cast(? as vector))
				""",
			UUID.randomUUID(),
			record.getId(),
			embeddingModel,
			toVectorLiteral(embedding)
		);
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
