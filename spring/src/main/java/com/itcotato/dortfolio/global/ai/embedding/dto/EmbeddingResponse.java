package com.itcotato.dortfolio.global.ai.embedding.dto;

public record EmbeddingResponse(
        String embeddingModel,
        float[] embedding
) {
}
