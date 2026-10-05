package com.itcotato.dortfolio.global.ai.observability.service;

import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.global.ai.observability.dto.AiUsageResponse;
import com.itcotato.dortfolio.global.ai.observability.entity.AiCallAttempt;
import com.itcotato.dortfolio.global.ai.observability.entity.AiCallStatus;
import com.itcotato.dortfolio.global.ai.observability.entity.AiFeature;
import com.itcotato.dortfolio.global.ai.observability.entity.AiRequest;
import com.itcotato.dortfolio.global.ai.observability.repository.AiCallAttemptRepository;
import com.itcotato.dortfolio.global.ai.observability.repository.AiRequestRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiRequestTracker {

    private final AiRequestRepository requestRepository;
    private final AiCallAttemptRepository attemptRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID start(UUID userId, AiFeature feature) {
        UUID requestId = UUID.randomUUID();

        User user = userId == null
                ? null
                : entityManager.getReference(User.class, userId);

        requestRepository.saveAndFlush(
                AiRequest.pending(requestId, user, feature)
        );
        return requestId;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(
            UUID requestId,
            int attemptNumber,
            AiUsageResponse usage
    ) {
        AiRequest request = findRequest(requestId);

        if (usage == null || !requestId.equals(usage.requestId())) {
            throw new IllegalArgumentException(
                    "AI usage requestId does not match."
            );
        }

        attemptRepository.save(
                AiCallAttempt.create(
                        request,
                        attemptNumber,
                        usage.provider(),
                        usage.modelId(),
                        usage.inputTokens(),
                        usage.outputTokens(),
                        null, // 비용 계산은 커밋 3
                        usage.latencyMs(),
                        AiCallStatus.SUCCESS,
                        null
                )
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(
            UUID requestId,
            int attemptNumber,
            String provider,
            String modelId,
            AiCallStatus status,
            String errorCode,
            long latencyMs
    ) {
        AiRequest request = findRequest(requestId);

        attemptRepository.save(
                AiCallAttempt.create(
                        request,
                        attemptNumber,
                        provider,
                        modelId,
                        null,
                        null,
                        null,
                        latencyMs,
                        status,
                        errorCode
                )
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finish(
            UUID requestId,
            boolean success,
            int retryCount,
            long totalLatencyMs
    ) {
        AiRequest request = findRequest(requestId);
        LocalDateTime completedAt = LocalDateTime.now();

        if (success) {
            request.complete(
                    retryCount,
                    totalLatencyMs,
                    null, // 비용 계산은 커밋 3
                    completedAt
            );
        } else {
            request.fail(
                    retryCount,
                    totalLatencyMs,
                    null,
                    completedAt
            );
        }
    }

    private AiRequest findRequest(UUID requestId) {
        return requestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "AI request not found: " + requestId
                ));
    }
}
