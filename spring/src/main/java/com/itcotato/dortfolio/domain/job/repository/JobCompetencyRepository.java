package com.itcotato.dortfolio.domain.job.repository;

import com.itcotato.dortfolio.domain.job.entity.JobCompetency;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobCompetencyRepository
        extends JpaRepository<JobCompetency, UUID> {

    @Query("""
    select jobCompetency.id
    from JobCompetency jobCompetency
    where not exists (
        select 1
        from JobCompetencyEmbedding embedding
        where embedding.jobCompetency = jobCompetency
          and embedding.embeddingModel = :embeddingModel
    )
    order by jobCompetency.job.id asc, jobCompetency.sortOrder asc
    """)
    List<UUID> findMissingEmbeddingIdsByModel(
            @Param("embeddingModel") String embeddingModel
    );

    @EntityGraph(attributePaths = {
            "job",
            "competencyTag"
    })
    Optional<JobCompetency> findWithDetailsById(UUID id);

    @EntityGraph(attributePaths = {
            "job",
            "competencyTag"
    })
    List<JobCompetency>
    findAllByJob_IdOrderBySortOrderAsc(UUID jobId);

    @EntityGraph(attributePaths = {
            "job",
            "competencyTag"
    })
    List<JobCompetency>
    findAllByOrderByJob_IdAscSortOrderAsc();
}
