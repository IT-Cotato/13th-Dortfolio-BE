package com.itcotato.dortfolio.domain.matching.service;

import com.itcotato.dortfolio.domain.matching.dto.fastapi.QuestionEmbeddingResponse;

public interface RecordMatchingClient {

	QuestionEmbeddingResponse embedQuestion(String question);
}
