package com.itcotato.dortfolio.domain.matching.service;

import com.itcotato.dortfolio.domain.matching.dto.fastapi.QuestionEmbeddingResponse;

import java.util.UUID;

public interface RecordMatchingClient {

	QuestionEmbeddingResponse embedQuestion(String question);

	default QuestionEmbeddingResponse embedQuestion(UUID requestId, String question) {
		return embedQuestion(question);
	}
}
