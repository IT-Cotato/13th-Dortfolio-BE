package com.itcotato.dortfolio.domain.job.repository;

import com.itcotato.dortfolio.domain.job.entity.JobCompetency;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobCompetencyRepository
        extends JpaRepository<JobCompetency, UUID> {

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
