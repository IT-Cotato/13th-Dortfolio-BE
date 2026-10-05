package com.itcotato.dortfolio.global.ai.observability.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.itcotato.dortfolio.global.ai.observability.dto.AiUsageResponse;
import com.itcotato.dortfolio.global.ai.observability.entity.AiCallStatus;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientResponseException;

class AiCallObserverTest {

    private final AiRequestTracker tracker = mock(AiRequestTracker.class);
    private final AiCallObserver observer = new AiCallObserver(
            tracker, new AiFailureMapper());

    @Test
    void recordsSuccessWithUsageMetadataOnly() {
        UUID requestId = UUID.randomUUID();
        AiUsageResponse usage = new AiUsageResponse(requestId, "GEMINI", "model", 12L, 3L, 15);

        String response = observer.attempt(requestId, 1, () -> "private response text", ignored -> usage);

        org.assertj.core.api.Assertions.assertThat(response).isEqualTo("private response text");
        verify(tracker).recordSuccess(requestId, 1, usage);
    }

    @Test
    void recordsOnlySafeMetadataFromMalformedHttpErrorBody() {
        UUID requestId = UUID.randomUUID();
        RestClientResponseException exception = new RestClientResponseException(
                "private prompt and answer", 502, "Bad Gateway", HttpHeaders.EMPTY,
                "private prompt and answer".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        assertThatThrownBy(() -> observer.attempt(requestId, 1,
                () -> { throw exception; }, ignored -> null))
                .isSameAs(exception);

        verify(tracker).recordFailure(eq(requestId), eq(1), eq("UNKNOWN"), eq("UNKNOWN"),
                eq(AiCallStatus.PROVIDER_ERROR), eq("HTTP_502"), anyLong());
    }

    @Test
    void invalidSchemaKeepsUsageForCostEstimationWithoutSavingBody() {
        UUID requestId = UUID.randomUUID();
        AiUsageResponse usage = new AiUsageResponse(requestId, "GEMINI", "model", 100L, 20L, 15);

        assertThatThrownBy(() -> observer.attempt(requestId, 2,
                () -> "private answer", ignored -> usage,
                ignored -> { throw new IllegalArgumentException("private answer"); }))
                .isInstanceOf(IllegalArgumentException.class);

        verify(tracker).recordFailure(eq(requestId), eq(2), eq(usage),
                eq(AiCallStatus.INVALID_RESPONSE), eq("SCHEMA_VALIDATION_FAILED"), anyLong());
    }
}
