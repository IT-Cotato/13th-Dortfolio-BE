package com.itcotato.dortfolio.domain.matching.dto.fastapi;

public record QuestionEmbeddingResponse(
	String embeddingModel,
	float[] embedding
) {
}
