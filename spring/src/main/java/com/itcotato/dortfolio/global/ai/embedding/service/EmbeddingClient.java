package com.itcotato.dortfolio.global.ai.embedding.service;

import com.itcotato.dortfolio.global.ai.embedding.dto.EmbeddingRequest;
import com.itcotato.dortfolio.global.ai.embedding.dto.EmbeddingResponse;

public interface EmbeddingClient {

    EmbeddingResponse embed(EmbeddingRequest request);
}
