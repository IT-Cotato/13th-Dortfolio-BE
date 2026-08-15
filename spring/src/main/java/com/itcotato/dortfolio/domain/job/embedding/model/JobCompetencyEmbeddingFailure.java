package com.itcotato.dortfolio.domain.job.embedding.model;

import java.util.UUID;

public record JobCompetencyEmbeddingFailure(
        UUID jobCompetencyId,
        String reason
) {
}
