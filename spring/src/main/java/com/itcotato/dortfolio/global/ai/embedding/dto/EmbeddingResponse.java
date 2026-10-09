package com.itcotato.dortfolio.global.ai.embedding.dto;

import com.itcotato.dortfolio.global.ai.observability.dto.AiUsageResponse;

public record EmbeddingResponse(
        String embeddingModel,
        float[] embedding,
        AiUsageResponse usage
) {
    public EmbeddingResponse(String embeddingModel, float[] embedding) {
        this(embeddingModel, embedding, null);
    }
}
