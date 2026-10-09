package com.itcotato.dortfolio.global.ai.observability.service;

import com.itcotato.dortfolio.global.ai.observability.dto.AiUsageResponse;
import com.itcotato.dortfolio.global.ai.observability.entity.AiCallStatus;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiCallObserver {

    private final AiRequestTracker tracker;
    private final AiFailureMapper failureMapper;

    public <T> T attempt(
            UUID requestId,
            int attemptNumber,
            Supplier<T> call,
            Function<T, AiUsageResponse> usageExtractor
    ) {
        return attempt(requestId, attemptNumber, call, usageExtractor, response -> {});
    }

    public <T> T attempt(
            UUID requestId,
            int attemptNumber,
            Supplier<T> call,
            Function<T, AiUsageResponse> usageExtractor,
            Consumer<T> validator
    ) {
        long started = System.nanoTime();
        try {
            T response = call.get();
            AiUsageResponse usage = response == null ? null : usageExtractor.apply(response);
            if (usage == null || !requestId.equals(usage.requestId())
                    || usage.provider() == null || usage.provider().isBlank()
                    || usage.modelId() == null || usage.modelId().isBlank()) {
                recordSafely(requestId, attemptNumber, () -> tracker.recordFailure(
                        requestId, attemptNumber, "UNKNOWN", "UNKNOWN",
                        AiCallStatus.INVALID_RESPONSE, "INVALID_USAGE", elapsed(started)));
                throw new InvalidAiUsageException();
            }
            try {
                validator.accept(response);
            } catch (RuntimeException exception) {
                recordSafely(requestId, attemptNumber, () -> tracker.recordFailure(
                        requestId, attemptNumber, usage,
                        AiCallStatus.INVALID_RESPONSE, "SCHEMA_VALIDATION_FAILED", elapsed(started)));
                throw exception;
            }
            recordSafely(requestId, attemptNumber,
                    () -> tracker.recordSuccess(requestId, attemptNumber, usage));
            return response;
        } catch (RestClientResponseException exception) {
            AiFailureMapper.Failure failure = failureMapper.fromHttpError(
                    requestId, exception, "UNKNOWN", "UNKNOWN", elapsed(started));
            recordSafely(requestId, attemptNumber, () -> tracker.recordFailure(
                    requestId, attemptNumber, failure.provider(), failure.modelId(),
                    failure.status(), failure.errorCode(), failure.latencyMs()));
            throw exception;
        } catch (RestClientException exception) {
            AiFailureMapper.Failure failure = failureMapper.fromTransportError(
                    exception, "UNKNOWN", "UNKNOWN", elapsed(started));
            recordSafely(requestId, attemptNumber, () -> tracker.recordFailure(
                    requestId, attemptNumber, failure.provider(), failure.modelId(),
                    failure.status(), failure.errorCode(), failure.latencyMs()));
            throw exception;
        }
    }

    private void recordSafely(UUID requestId, int attemptNumber, Runnable recording) {
        try {
            recording.run();
        } catch (RuntimeException trackingFailure) {
            log.warn("AI call tracking failed: requestId={}, attemptNumber={}, exceptionType={}",
                    requestId, attemptNumber, trackingFailure.getClass().getSimpleName());
        }
    }

    public long elapsed(long started) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    }

    public static final class InvalidAiUsageException extends RuntimeException {
    }
}
