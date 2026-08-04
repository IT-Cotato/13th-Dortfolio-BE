package com.itcotato.dortfolio.domain.job.embedding.service;

import com.itcotato.dortfolio.domain.job.embedding.dto.EmbeddingRequest;
import com.itcotato.dortfolio.domain.job.embedding.dto.EmbeddingResponse;

public interface EmbeddingClient {

    EmbeddingResponse embed(EmbeddingRequest request);
}
