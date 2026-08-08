package com.itcotato.dortfolio.domain.insight.entity;

import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "insights")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Insight extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "job_id_snapshot", nullable = false)
    private UUID jobIdSnapshot;

    @Column(name = "job_name_snapshot", nullable = false)
    private String jobNameSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InsightGenerationStatus status;

    @Column(name = "record_snapshot_at", nullable = false)
    private LocalDateTime recordSnapshotAt;

    @Column(name = "base_completed_record_count", nullable = false)
    private int baseCompletedRecordCount;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

    @Column
    private LocalDateTime failedAt;

    @Column(length = 100)
    private String failureCode;

    @Column(columnDefinition = "text")
    private String failureMessage;

    private Insight(
            User user,
            UUID jobIdSnapshot,
            String jobNameSnapshot,
            LocalDateTime recordSnapshotAt,
            int baseCompletedRecordCount,
            LocalDateTime requestedAt
    ) {
        this.user = user;
        this.jobIdSnapshot = jobIdSnapshot;
        this.jobNameSnapshot = jobNameSnapshot;
        this.status = InsightGenerationStatus.PENDING;
        this.recordSnapshotAt = recordSnapshotAt;
        this.baseCompletedRecordCount = baseCompletedRecordCount;
        this.requestedAt = requestedAt;
    }

    public static Insight pending(
            User user,
            UUID jobIdSnapshot,
            String jobNameSnapshot,
            LocalDateTime recordSnapshotAt,
            int baseCompletedRecordCount,
            LocalDateTime requestedAt
    ) {
        return new Insight(
                user,
                jobIdSnapshot,
                jobNameSnapshot,
                recordSnapshotAt,
                baseCompletedRecordCount,
                requestedAt
        );
    }

    public void complete(LocalDateTime completedAt) {
        if (status != InsightGenerationStatus.PENDING
                && status != InsightGenerationStatus.RUNNING) {
            throw new IllegalStateException(
                    "Only active Insight can be completed."
            );
        }

        this.status = InsightGenerationStatus.COMPLETED;
        this.completedAt = completedAt;
        this.failedAt = null;
        this.failureCode = null;
        this.failureMessage = null;
    }

    public void fail(
            LocalDateTime failedAt,
            String failureCode,
            String failureMessage
    ) {
        validateActive();

        this.status = InsightGenerationStatus.FAILED;
        this.completedAt = null;
        this.failedAt = failedAt;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
    }

    public void start(LocalDateTime startedAt) {
        if (status != InsightGenerationStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending Insight can start."
            );
        }

        this.status = InsightGenerationStatus.RUNNING;
        this.startedAt = startedAt;
    }

    private void validateActive() {
        if (status != InsightGenerationStatus.PENDING
                && status != InsightGenerationStatus.RUNNING) {
            throw new IllegalStateException(
                    "Only active Insight can fail."
            );
        }
    }
}
