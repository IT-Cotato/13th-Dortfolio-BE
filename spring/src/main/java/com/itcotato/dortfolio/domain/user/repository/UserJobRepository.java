package com.itcotato.dortfolio.domain.user.repository;

import com.itcotato.dortfolio.domain.user.entity.UserJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserJobRepository extends JpaRepository<UserJob, Long> {

    @Query("SELECT uj FROM UserJob uj JOIN FETCH uj.job WHERE uj.user.id = :userId AND uj.isPrimary = true")
    Optional<UserJob> findByUserIdAndIsPrimaryTrue(@Param("userId") UUID userId);

    Optional<UserJob> findByUserIdAndJobId(UUID userId, Long jobId);
}