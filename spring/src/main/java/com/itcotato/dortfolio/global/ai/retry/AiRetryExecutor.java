package com.itcotato.dortfolio.global.ai.retry;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiRetryExecutor {
    private final AiRetryPolicy policy;
    private final AiRetryProperties properties;
    private final AiRetrySleeper sleeper;
    private final MeterRegistry meterRegistry;

    public <T> T execute(String feature, Supplier<T> call) {
        return execute(feature, call, properties.maxAttempts(), properties.initialBackoff());
    }

    public <T> T execute(String feature, Supplier<T> call, int maxAttempts, Duration initialBackoff) {
        if (maxAttempts < 1 || initialBackoff == null || initialBackoff.isNegative()) {
            throw new IllegalArgumentException("Invalid AI retry policy");
        }
        for (int attempt = 1; ; attempt++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new ResourceAccessException("AI request interrupted");
            }
            meterRegistry.counter("ai.call.attempts", "feature", feature).increment();
            try {
                return call.get();
            } catch (RestClientException exception) {
                AiFailureCode failure = policy.classify(exception);
                if (failure != AiFailureCode.TEMPORARY || attempt >= maxAttempts) {
                    throw exception;
                }
                Duration delay = policy.backoff(attempt, initialBackoff, exception);
                log.warn("AI call retry scheduled. feature={}, attempt={}, maxAttempts={}, delay={}, failureCode={}",
                        feature, attempt, maxAttempts, delay, failure.getCode());
                meterRegistry.counter("ai.call.retries", "feature", feature).increment();
                sleeper.sleep(delay);
            }
        }
    }
}
