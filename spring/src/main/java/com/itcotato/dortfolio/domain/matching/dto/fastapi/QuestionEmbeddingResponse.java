package com.itcotato.dortfolio.domain.matching.dto.fastapi;

import com.itcotato.dortfolio.global.ai.observability.dto.AiUsageResponse;

public record QuestionEmbeddingResponse(
	String embeddingModel,
	float[] embedding,
    AiUsageResponse usage
) {
	public QuestionEmbeddingResponse(String embeddingModel, float[] embedding) {
		this(embeddingModel, embedding, null);
	}
}
