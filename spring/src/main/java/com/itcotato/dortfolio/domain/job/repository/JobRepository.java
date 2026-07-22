package com.itcotato.dortfolio.domain.job.repository;

import com.itcotato.dortfolio.domain.job.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {
}