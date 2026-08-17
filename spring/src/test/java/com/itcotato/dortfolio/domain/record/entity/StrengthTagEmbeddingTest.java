package com.itcotato.dortfolio.domain.record.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StrengthTagEmbeddingTest {

	@Test
	void returnsDefensiveCopyOfEmbedding() {
		float[] source = new float[StrengthTagEmbedding.EMBEDDING_DIMENSION];
		source[0] = 1.0f;
		StrengthTagEmbedding embedding = StrengthTagEmbedding.create(
			StrengthTag.create("TEST", "테스트", "설명", "기준", "적합", "부적합"),
			"test-model",
			source
		);

		source[0] = 2.0f;
		float[] returned = embedding.getEmbedding();
		returned[0] = 3.0f;

		assertThat(embedding.getEmbedding()[0]).isEqualTo(1.0f);
	}
}
