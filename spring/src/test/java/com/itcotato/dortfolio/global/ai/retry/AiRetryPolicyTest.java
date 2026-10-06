package com.itcotato.dortfolio.global.ai.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

class AiRetryPolicyTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-06T06:00:00Z"), ZoneOffset.UTC);
    private final AiRetryPolicy policy = new AiRetryPolicy(clock);

    @Test
    void typedPermanentFailuresOverrideRetryableHttpStatuses() {
        assertThat(policy.classify(error(502, "{\"status\":\"INVALID_RESPONSE\"}", null)))
                .isEqualTo(AiFailureCode.INVALID_RESPONSE);
        assertThat(policy.classify(error(503, "{\"detail\":\"GEMINI_API_KEY is required.\"}", null)))
                .isEqualTo(AiFailureCode.CONFIGURATION);
        assertThat(policy.classify(error(503, "{\"status\":\"CONFIGURATION_ERROR\"}", null)))
                .isEqualTo(AiFailureCode.CONFIGURATION);
        assertThat(policy.classify(error(502, "{\"detail\":{\"errorCode\":\"OUTPUT_TOKEN_LIMIT\"}}", null)))
                .isEqualTo(AiFailureCode.OUTPUT_LIMIT);
        assertThat(policy.classify(error(502, "{\"requestId\":\"id\",\"status\":\"INVALID_RESPONSE\",\"errorCode\":\"SCHEMA_VALIDATION_FAILED\"}", null)))
                .isEqualTo(AiFailureCode.INVALID_RESPONSE);
    }

    @Test
    void recognizesOnlyTemporaryHttpAndConnectionFailures() {
        for (int code : new int[]{408, 429, 500, 502, 503, 504}) {
            assertThat(policy.classify(error(code, "", null))).isEqualTo(AiFailureCode.TEMPORARY);
        }
        for (int code : new int[]{400, 422, 501, 505}) {
            assertThat(policy.classify(error(code, "", null))).isEqualTo(AiFailureCode.REJECTED);
        }
        assertThat(policy.classify(error(401, "", null))).isEqualTo(AiFailureCode.CONFIGURATION);
        assertThat(policy.classify(new ResourceAccessException("timeout"))).isEqualTo(AiFailureCode.TEMPORARY);
    }

    @Test
    void respectsBothRetryAfterFormatsAndIgnoresInvalidOrPastValues() {
        assertThat(policy.retryAfter(error(429, "", "30"))).isEqualTo(Duration.ofSeconds(30));
        assertThat(policy.retryAfter(error(429, "", "Tue, 06 Oct 2026 06:01:00 GMT"))).isEqualTo(Duration.ofMinutes(1));
        for (String value : new String[]{"-1", "invalid", "9999999999999999999", "Tue, 06 Oct 2026 05:00:00 GMT"}) {
            assertThat(policy.retryAfter(error(429, "", value))).isEqualTo(Duration.ZERO);
        }
        assertThat(policy.backoff(2, Duration.ofSeconds(2), error(429, "", "30")))
                .isBetween(Duration.ofSeconds(30), Duration.ofSeconds(32));
        assertThat(policy.backoff(1, Duration.ofSeconds(2),
                error(429, "", Long.toString(Long.MAX_VALUE / 1000))).toMillis()).isPositive();
        assertThat(policy.backoff(2, Duration.ofSeconds(2), error(500, "", null)))
                .isBetween(Duration.ofSeconds(4), Duration.ofSeconds(6));
    }

    @Test
    void capsAttemptsAtThreeAndKeepsFailureAvailableForManualRetry() {
        AiRetrySleeper sleeper = mock(AiRetrySleeper.class);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AiRetryExecutor executor = new AiRetryExecutor(policy, new AiRetryProperties(3, Duration.ZERO), sleeper, registry);
        AtomicInteger attempts = new AtomicInteger();
        assertThatThrownBy(() -> executor.execute("record_analysis", () -> {
            attempts.incrementAndGet();
            throw new ResourceAccessException("timeout");
        })).isInstanceOf(ResourceAccessException.class);
        assertThat(attempts.get()).isEqualTo(3);
        verify(sleeper, times(2)).sleep(Duration.ZERO);
        assertThat(registry.get("ai.call.attempts").counter().count()).isEqualTo(3);
        assertThat(registry.get("ai.call.retries").counter().count()).isEqualTo(2);
    }

    @Test
    void doesNotRepeatInvalidResponses() {
        AiRetrySleeper sleeper = mock(AiRetrySleeper.class);
        AiRetryExecutor executor = new AiRetryExecutor(policy, new AiRetryProperties(3, Duration.ZERO), sleeper, new SimpleMeterRegistry());
        AtomicInteger attempts = new AtomicInteger();
        assertThatThrownBy(() -> executor.execute("insight_recommendation", () -> {
            attempts.incrementAndGet();
            throw error(502, "{\"status\":\"INVALID_RESPONSE\"}", null);
        })).isInstanceOf(RestClientResponseException.class);
        assertThat(attempts.get()).isEqualTo(1);
        org.mockito.Mockito.verifyNoInteractions(sleeper);
    }

    @Test
    void interruptionStopsCallsAndPreservesInterruptFlag() {
        AiRetryExecutor executor = new AiRetryExecutor(policy, new AiRetryProperties(3, Duration.ZERO), new AiRetrySleeper(), new SimpleMeterRegistry());
        AtomicInteger attempts = new AtomicInteger();
        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> executor.execute("record_analysis", () -> attempts.incrementAndGet()))
                    .isInstanceOf(ResourceAccessException.class);
            assertThat(attempts.get()).isZero();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    private RestClientResponseException error(int code, String body, String retryAfter) {
        HttpHeaders headers = new HttpHeaders();
        if (retryAfter != null) {
            headers.set(HttpHeaders.RETRY_AFTER, retryAfter);
        }
        return new RestClientResponseException("AI request failed", code, "error", headers,
                body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}
