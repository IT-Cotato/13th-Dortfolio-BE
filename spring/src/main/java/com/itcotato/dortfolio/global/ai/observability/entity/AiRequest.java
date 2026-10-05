package com.itcotato.dortfolio.global.ai.observability.entity;

import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "ai_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiRequest extends BaseEntity {
    @Column(name = "request_id", nullable = false, unique = true, updatable = false)
    private UUID requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AiFeature feature;

    @Enumerated(EnumType.STRING)
    @Column(name = "final_status", nullable = false, length = 30)
    private AiRequestStatus finalStatus;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "total_latency_ms")
    private Long totalLatencyMs;

    @Column(
            name = "total_estimated_cost_usd",
            precision = 18,
            scale = 10
    )
    private BigDecimal totalEstimatedCostUsd;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    private AiRequest(
            UUID requestId,
            User user,
            AiFeature feature
    ) {
        this.requestId = requestId;
        this.user = user;
        this.feature = feature;
        this.finalStatus = AiRequestStatus.PENDING;
        this.retryCount = 0;
    }

    public static AiRequest pending(
            UUID requestId,
            User user,
            AiFeature feature
    ) {
        if (requestId == null) {
            throw new IllegalArgumentException(
                    "requestId must not be null."
            );
        }
        if (feature == null) {
            throw new IllegalArgumentException(
                    "feature must not be null."
            );
        }
        return new AiRequest(requestId, user, feature);
    }

    public void complete(
            int retryCount,
            long totalLatencyMs,
            BigDecimal totalEstimatedCostUsd,
            LocalDateTime completedAt
    ) {
        validatePending();
        validateCompletionValues(
                retryCount,
                totalLatencyMs,
                totalEstimatedCostUsd,
                completedAt
        );

        this.finalStatus = AiRequestStatus.SUCCESS;
        this.retryCount = retryCount;
        this.totalLatencyMs = totalLatencyMs;
        this.totalEstimatedCostUsd = totalEstimatedCostUsd;
        this.completedAt = completedAt;
    }

    public void fail(
            int retryCount,
            long totalLatencyMs,
            BigDecimal totalEstimatedCostUsd,
            LocalDateTime completedAt
    ) {
        validatePending();
        validateCompletionValues(
                retryCount,
                totalLatencyMs,
                totalEstimatedCostUsd,
                completedAt
        );

        this.finalStatus = AiRequestStatus.FAILED;
        this.retryCount = retryCount;
        this.totalLatencyMs = totalLatencyMs;
        this.totalEstimatedCostUsd = totalEstimatedCostUsd;
        this.completedAt = completedAt;
    }

    private void validatePending() {
        if (finalStatus != AiRequestStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Only pending AI request can be completed."
            );
        }
    }

    private void validateCompletionValues(
            int retryCount,
            long totalLatencyMs,
            BigDecimal totalEstimatedCostUsd,
            LocalDateTime completedAt
    ) {
        if (retryCount < 0) {
            throw new IllegalArgumentException(
                    "retryCount must not be negative."
            );
        }
        if (totalLatencyMs < 0) {
            throw new IllegalArgumentException(
                    "totalLatencyMs must not be negative."
            );
        }
        if (totalEstimatedCostUsd != null
                && totalEstimatedCostUsd.signum() < 0) {
            throw new IllegalArgumentException(
                    "totalEstimatedCostUsd must not be negative."
            );
        }
        if (completedAt == null) {
            throw new IllegalArgumentException(
                    "completedAt must not be null."
            );
        }
    }
}
