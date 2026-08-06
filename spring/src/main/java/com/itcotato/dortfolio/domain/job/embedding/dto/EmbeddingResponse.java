package com.itcotato.dortfolio.domain.job.embedding.dto;

public record EmbeddingResponse(
        String embeddingModel,
        float[] embedding
) {
}
