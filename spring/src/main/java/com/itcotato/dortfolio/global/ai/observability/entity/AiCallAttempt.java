package com.itcotato.dortfolio.global.ai.observability.entity;

import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "ai_call_attempts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiCallAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_request_id", nullable = false)
    private AiRequest aiRequest;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "model_id", nullable = false, length = 255)
    private String modelId;

    @Column(name = "input_tokens")
    private Long inputTokens;

    @Column(name = "output_tokens")
    private Long outputTokens;

    @Column(
            name = "estimated_cost_usd",
            precision = 18,
            scale = 10
    )
    private BigDecimal estimatedCostUsd;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiCallStatus status;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    private AiCallAttempt(
            AiRequest aiRequest,
            int attemptNumber,
            String provider,
            String modelId,
            Long inputTokens,
            Long outputTokens,
            BigDecimal estimatedCostUsd,
            Long latencyMs,
            AiCallStatus status,
            String errorCode
    ) {
        validate(
                aiRequest,
                attemptNumber,
                provider,
                modelId,
                inputTokens,
                outputTokens,
                estimatedCostUsd,
                latencyMs,
                status
        );

        this.aiRequest = aiRequest;
        this.attemptNumber = attemptNumber;
        this.provider = provider;
        this.modelId = modelId;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.estimatedCostUsd = estimatedCostUsd;
        this.latencyMs = latencyMs;
        this.status = status;
        this.errorCode = normalizeErrorCode(errorCode);
    }

    public static AiCallAttempt create(
            AiRequest aiRequest,
            int attemptNumber,
            String provider,
            String modelId,
            Long inputTokens,
            Long outputTokens,
            BigDecimal estimatedCostUsd,
            Long latencyMs,
            AiCallStatus status,
            String errorCode
    ) {
        return new AiCallAttempt(
                aiRequest,
                attemptNumber,
                provider,
                modelId,
                inputTokens,
                outputTokens,
                estimatedCostUsd,
                latencyMs,
                status,
                errorCode
        );
    }

    private void validate(
            AiRequest aiRequest,
            int attemptNumber,
            String provider,
            String modelId,
            Long inputTokens,
            Long outputTokens,
            BigDecimal estimatedCostUsd,
            Long latencyMs,
            AiCallStatus status
    ) {
        if (aiRequest == null) {
            throw new IllegalArgumentException(
                    "aiRequest must not be null."
            );
        }
        if (attemptNumber < 1) {
            throw new IllegalArgumentException(
                    "attemptNumber must be at least 1."
            );
        }
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException(
                    "provider must not be blank."
            );
        }
        if (modelId == null || modelId.isBlank()) {
            throw new IllegalArgumentException(
                    "modelId must not be blank."
            );
        }
        if (inputTokens != null && inputTokens < 0) {
            throw new IllegalArgumentException(
                    "inputTokens must not be negative."
            );
        }
        if (outputTokens != null && outputTokens < 0) {
            throw new IllegalArgumentException(
                    "outputTokens must not be negative."
            );
        }
        if (estimatedCostUsd != null
                && estimatedCostUsd.signum() < 0) {
            throw new IllegalArgumentException(
                    "estimatedCostUsd must not be negative."
            );
        }
        if (latencyMs != null && latencyMs < 0) {
            throw new IllegalArgumentException(
                    "latencyMs must not be negative."
            );
        }
        if (status == null) {
            throw new IllegalArgumentException(
                    "status must not be null."
            );
        }
    }

    private String normalizeErrorCode(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            return null;
        }
        return errorCode;
    }

}
