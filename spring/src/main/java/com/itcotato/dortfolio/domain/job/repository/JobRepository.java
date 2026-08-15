package com.itcotato.dortfolio.domain.job.repository;

import com.itcotato.dortfolio.domain.job.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {
    List<Job> findAllByOrderByCodeAsc();
}