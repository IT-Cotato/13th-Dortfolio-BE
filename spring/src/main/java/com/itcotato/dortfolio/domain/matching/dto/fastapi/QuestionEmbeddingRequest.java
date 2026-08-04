package com.itcotato.dortfolio.domain.matching.dto.fastapi;

public record QuestionEmbeddingRequest(
	String question
) {

	public static QuestionEmbeddingRequest of(String question) {
		return new QuestionEmbeddingRequest(question);
	}
}
