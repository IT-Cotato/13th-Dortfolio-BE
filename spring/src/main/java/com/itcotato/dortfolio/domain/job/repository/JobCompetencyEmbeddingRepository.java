package com.itcotato.dortfolio.domain.job.repository;

import com.itcotato.dortfolio.domain.job.entity.JobCompetencyEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JobCompetencyEmbeddingRepository extends JpaRepository<JobCompetencyEmbedding, UUID> {

    long countByEmbeddingModel(String embeddingModel);

    boolean existsByJobCompetency_IdAndEmbeddingModel(
            UUID jobCompetencyId,
            String embeddingModel
    );

    Optional<JobCompetencyEmbedding>
            findByJobCompetency_IdAndEmbeddingModel(
                    UUID jobCompetencyId,
                    String embeddingModel
    );
}
