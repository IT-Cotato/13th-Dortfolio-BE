package com.itcotato.dortfolio.domain.job.embedding.service;

import java.util.UUID;

public interface JobCompetencyEmbeddingWriter {

    // 동일 직무 역량과 모델이 이미 존재하면 저장 X
    boolean saveIfAbsent(
            UUID jobCompetencyId,
            String embeddingModel,
            float[] embedding
    );
}
