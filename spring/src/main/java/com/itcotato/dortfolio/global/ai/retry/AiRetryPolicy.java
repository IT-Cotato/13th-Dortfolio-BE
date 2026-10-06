package com.itcotato.dortfolio.global.ai.retry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class AiRetryPolicy {
    private final Clock clock;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiFailureCode classify(RestClientException exception) {
        if (exception instanceof RestClientResponseException response) {
            String providerStatus = providerStatus(response);
            if ("INVALID_RESPONSE".equals(providerStatus)) {
                return AiFailureCode.INVALID_RESPONSE;
            }
            if ("CONFIGURATION_ERROR".equals(providerStatus)) {
                return AiFailureCode.CONFIGURATION;
            }
            if ("OUTPUT_LIMIT".equals(providerStatus)) {
                return AiFailureCode.OUTPUT_LIMIT;
            }
            int code = response.getStatusCode().value();
            if (code == 401 || code == 403) {
                return AiFailureCode.CONFIGURATION;
            }
            return code == 408 || code == 429 || code == 500 || code == 502 || code == 503 || code == 504
                    ? AiFailureCode.TEMPORARY : AiFailureCode.REJECTED;
        }
        return exception instanceof ResourceAccessException
                ? AiFailureCode.TEMPORARY : AiFailureCode.INVALID_RESPONSE;
    }

    private String providerStatus(RestClientResponseException exception) {
        try {
            JsonNode body = objectMapper.readTree(exception.getResponseBodyAsByteArray());
            if (body == null) {
                return null;
            }
            if ("GEMINI_API_KEY is required.".equals(body.path("detail").asText())) {
                return "CONFIGURATION_ERROR";
            }
            // Also accept FastAPI's HTTPException envelope during rolling upgrades.
            JsonNode error = body.has("status") ? body : body.path("detail");
            String status = error.path("status").asText();
            String code = error.path("errorCode").asText();
            if ("SCHEMA_VALIDATION_FAILED".equals(code)) {
                return "INVALID_RESPONSE";
            }
            if ("API_KEY_MISSING".equals(code)) {
                return "CONFIGURATION_ERROR";
            }
            if ("OUTPUT_TOKEN_LIMIT".equals(code)) {
                return "OUTPUT_LIMIT";
            }
            return status;
        } catch (Exception ignored) {
            return null;
        }
    }

    public Duration backoff(int attempt, Duration initialBackoff, RestClientException exception) {
        Duration configured = initialBackoff.multipliedBy(1L << Math.min(attempt - 1, 10));
        Duration retryAfter = retryAfter(exception);
        Duration base = configured.compareTo(retryAfter) >= 0 ? configured : retryAfter;
        if (base.isZero()) {
            return base;
        }
        // Jitter is based on our backoff, not an untrusted Retry-After value.
        long bound = Math.min(Math.max(1L, configured.toMillis() / 2), Long.MAX_VALUE - base.toMillis());
        return base.plusMillis(ThreadLocalRandom.current().nextLong(bound + 1));
    }

    public Duration retryAfter(RestClientException exception) {
        if (!(exception instanceof RestClientResponseException response) || response.getResponseHeaders() == null) {
            return Duration.ZERO;
        }
        String value = response.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER);
        if (value == null || value.isBlank()) {
            return Duration.ZERO;
        }
        try {
            long seconds = Long.parseLong(value.trim());
            // Guard Duration.toMillis used by sleepers against numeric overflow.
            if (seconds > Long.MAX_VALUE / 1000) {
                return Duration.ZERO;
            }
            return Duration.ofSeconds(Math.max(0L, seconds));
        } catch (NumberFormatException ignored) {
            try {
                Duration delay = Duration.between(clock.instant(),
                        ZonedDateTime.parse(value.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant());
                delay.toMillis();
                return delay.isNegative() ? Duration.ZERO : delay;
            } catch (DateTimeParseException | ArithmeticException ignoredDate) {
                return Duration.ZERO;
            }
        }
    }
}
