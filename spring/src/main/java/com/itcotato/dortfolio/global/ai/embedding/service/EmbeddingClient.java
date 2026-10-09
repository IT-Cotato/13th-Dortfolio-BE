package com.itcotato.dortfolio.global.ai.embedding.service;

import com.itcotato.dortfolio.global.ai.embedding.dto.EmbeddingRequest;
import com.itcotato.dortfolio.global.ai.embedding.dto.EmbeddingResponse;

import java.util.UUID;

public interface EmbeddingClient {

    EmbeddingResponse embed(EmbeddingRequest request);

    default EmbeddingResponse embed(UUID requestId, EmbeddingRequest request) {
        return embed(request);
    }
}
