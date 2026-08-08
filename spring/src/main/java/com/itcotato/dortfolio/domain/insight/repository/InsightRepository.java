package com.itcotato.dortfolio.domain.insight.repository;

import com.itcotato.dortfolio.domain.insight.entity.Insight;
import com.itcotato.dortfolio.domain.insight.entity.InsightGenerationStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsightRepository extends JpaRepository<Insight, UUID> {

    Optional<Insight>
    findFirstByUser_IdAndStatusAndCompletedAtIsNotNullOrderByCompletedAtDescIdDesc(
            UUID userId,
            InsightGenerationStatus status
    );

    Optional<Insight>
    findFirstByUser_IdAndStatusOrderByRequestedAtDescIdDesc(
            UUID userId,
            InsightGenerationStatus status
    );

    Optional<Insight>
    findFirstByUser_IdAndStatusInOrderByRequestedAtDescIdDesc(
            UUID userId,
            List<InsightGenerationStatus> statuses
    );

    Optional<Insight> findByIdAndUser_Id(UUID insightId, UUID userId);

    default Optional<Insight> findLatestCompletedByUserId(UUID userId) {
        return findFirstByUser_IdAndStatusAndCompletedAtIsNotNullOrderByCompletedAtDescIdDesc(
                userId,
                InsightGenerationStatus.COMPLETED
        );
    }

    default Optional<Insight> findPendingByUserId(UUID userId) {
        return findFirstByUser_IdAndStatusInOrderByRequestedAtDescIdDesc(
                userId,
                List.of(
                        InsightGenerationStatus.PENDING,
                        InsightGenerationStatus.RUNNING
                )
        );
    }

    default Optional<Insight> findActiveByUserId(UUID userId) {
        return findPendingByUserId(userId);
    }

    boolean existsByUser_IdAndStatus(
            UUID userId,
            InsightGenerationStatus status
    );

    boolean existsByUser_IdAndStatusIn(
            UUID userId,
            List<InsightGenerationStatus> statuses
    );

    List<Insight> findAllByStatusAndStartedAtBefore(
            InsightGenerationStatus status,
            LocalDateTime startedBefore
    );
}
