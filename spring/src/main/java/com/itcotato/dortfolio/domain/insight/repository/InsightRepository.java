package com.itcotato.dortfolio.domain.insight.repository;

import com.itcotato.dortfolio.domain.insight.entity.Insight;
import com.itcotato.dortfolio.domain.insight.entity.InsightGenerationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InsightRepository extends JpaRepository<Insight, UUID> {

    Optional<Insight> findFirstByUser_IdAndStatusOrderByCompletedAtDesc(
            UUID userId,
            InsightGenerationStatus status
    );

    default Optional<Insight> findLatestCompletedByUserId(UUID userId) {
        return findFirstByUser_IdAndStatusOrderByCompletedAtDesc(
                userId,
                InsightGenerationStatus.COMPLETED
        );
    }

    boolean existsByUser_IdAndStatus(
            UUID userId,
            InsightGenerationStatus status
    );
}
