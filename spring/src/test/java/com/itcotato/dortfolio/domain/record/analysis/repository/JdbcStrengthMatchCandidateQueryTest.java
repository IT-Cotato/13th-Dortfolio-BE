package com.itcotato.dortfolio.domain.record.analysis.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.itcotato.dortfolio.domain.record.entity.StrengthTagEmbedding;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcStrengthMatchCandidateQueryTest {

	private final JdbcStrengthMatchCandidateQuery query =
		new JdbcStrengthMatchCandidateQuery(mock(JdbcTemplate.class));

	@Test
	void rejectsRecordEmbeddingWithoutMagnitude() {
		float[] embedding = new float[StrengthTagEmbedding.EMBEDDING_DIMENSION];

		assertThatThrownBy(() -> query.findTopCandidates("test-model", embedding, 5, 0.4))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("non-zero magnitude");
	}
}
