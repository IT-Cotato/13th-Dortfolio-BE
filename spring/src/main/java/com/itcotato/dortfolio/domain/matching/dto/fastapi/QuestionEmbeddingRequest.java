package com.itcotato.dortfolio.domain.matching.dto.fastapi;

public record QuestionEmbeddingRequest(
	String question,
	String inputType
) {

	// 저장된 기록(search_document)을 찾기 위한 질의이므로 검색 질의용 임베딩을 요청한다.
	private static final String SEARCH_QUERY_INPUT_TYPE = "search_query";

	public static QuestionEmbeddingRequest of(String question) {
		return new QuestionEmbeddingRequest(question, SEARCH_QUERY_INPUT_TYPE);
	}
}
