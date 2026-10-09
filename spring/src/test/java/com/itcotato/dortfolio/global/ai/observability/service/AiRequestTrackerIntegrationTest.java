package com.itcotato.dortfolio.global.ai.observability.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.itcotato.dortfolio.global.ai.observability.dto.AiUsageResponse;
import com.itcotato.dortfolio.global.ai.observability.entity.AiCallAttempt;
import com.itcotato.dortfolio.global.ai.observability.entity.AiCallStatus;
import com.itcotato.dortfolio.global.ai.observability.entity.AiFeature;
import com.itcotato.dortfolio.global.ai.observability.entity.AiRequest;
import com.itcotato.dortfolio.global.ai.observability.entity.AiRequestStatus;
import com.itcotato.dortfolio.global.ai.observability.repository.AiCallAttemptRepository;
import com.itcotato.dortfolio.global.ai.observability.repository.AiRequestRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientResponseException;

@SpringBootTest
@ActiveProfiles("test")
class AiRequestTrackerIntegrationTest {

    @Autowired private AiRequestTracker tracker;
    @Autowired private AiCallObserver observer;
    @Autowired private AiRequestRepository requests;
    @Autowired private AiCallAttemptRepository attempts;
    @Autowired private TransactionTemplate transactions;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void cleanUp() {
        attempts.deleteAll();
        requests.deleteAll();
    }

    @Test
    void successStoresTokensCostLatencyAndStatus() {
        UUID requestId = tracker.start(null, AiFeature.RECORD_EMBEDDING);
        tracker.recordSuccess(requestId, 1, usage(requestId, "gemini-embedding-2", 1_000L));
        tracker.finish(requestId, true, 0, 42);

        AiRequest request = requests.findByRequestId(requestId).orElseThrow();
        AiCallAttempt attempt = onlyAttempt(request);
        assertThat(request.getFinalStatus()).isEqualTo(AiRequestStatus.SUCCESS);
        assertThat(request.getRetryCount()).isZero();
        assertThat(request.getTotalLatencyMs()).isEqualTo(42);
        assertThat(request.getTotalEstimatedCostUsd()).isEqualByComparingTo("0.0002000000");
        assertThat(request.getCompletedAt()).isNotNull();
        assertThat(attempt.getInputTokens()).isEqualTo(1_000L);
        assertThat(attempt.getOutputTokens()).isNull();
        assertThat(attempt.getStatus()).isEqualTo(AiCallStatus.SUCCESS);
        assertThat(attempt.getEstimatedCostUsd()).isEqualByComparingTo("0.0002000000");
    }

    @Test
    void failureStoresSafeErrorButDoesNotInventCost() {
        UUID requestId = tracker.start(null, AiFeature.RECORD_ANALYSIS);
        tracker.recordFailure(requestId, 1, "GEMINI", "gemini-3.6-flash",
                AiCallStatus.RATE_LIMITED, "HTTP_429", 20);
        tracker.finish(requestId, false, 0, 25);

        AiRequest request = requests.findByRequestId(requestId).orElseThrow();
        AiCallAttempt attempt = onlyAttempt(request);
        assertThat(request.getFinalStatus()).isEqualTo(AiRequestStatus.FAILED);
        assertThat(request.getTotalEstimatedCostUsd()).isNull();
        assertThat(attempt.getStatus()).isEqualTo(AiCallStatus.RATE_LIMITED);
        assertThat(attempt.getErrorCode()).isEqualTo("HTTP_429");
        assertThat(attempt.getInputTokens()).isNull();
        assertThat(attempt.getEstimatedCostUsd()).isNull();
    }

    @Test
    void retryAggregatesKnownAttemptCostsAndRetryCount() {
        UUID requestId = tracker.start(null, AiFeature.INSIGHT_RECOMMENDATION);
        tracker.recordFailure(requestId, 1, usage(requestId, "gemini-embedding-2", 1_000L),
                AiCallStatus.INVALID_RESPONSE, "SCHEMA_VALIDATION_FAILED", 12);
        tracker.recordSuccess(requestId, 2, usage(requestId, "gemini-embedding-2", 2_000L));
        tracker.finish(requestId, true, 1, 60);

        AiRequest request = requests.findByRequestId(requestId).orElseThrow();
        List<AiCallAttempt> saved = attempts.findAllByAiRequest_IdOrderByAttemptNumberAsc(request.getId());
        assertThat(saved).extracting(AiCallAttempt::getAttemptNumber).containsExactly(1, 2);
        assertThat(saved).extracting(AiCallAttempt::getStatus)
                .containsExactly(AiCallStatus.INVALID_RESPONSE, AiCallStatus.SUCCESS);
        assertThat(request.getRetryCount()).isEqualTo(1);
        assertThat(request.getTotalLatencyMs()).isEqualTo(60);
        assertThat(request.getTotalEstimatedCostUsd()).isEqualByComparingTo("0.0006000000");
    }

    @Test
    void unknownModelLeavesAttemptAndTotalCostUnknown() {
        UUID requestId = tracker.start(null, AiFeature.QUESTION_EMBEDDING);
        tracker.recordSuccess(requestId, 1, usage(requestId, "unregistered-model", 1_000L));
        tracker.finish(requestId, true, 0, 10);

        AiRequest request = requests.findByRequestId(requestId).orElseThrow();
        assertThat(onlyAttempt(request).getEstimatedCostUsd()).isNull();
        assertThat(request.getTotalEstimatedCostUsd()).isNull();
    }

    @Test
    void requiresNewKeepsHistoryWhenCallerTransactionRollsBack() {
        UUID[] requestId = new UUID[1];
        transactions.executeWithoutResult(status -> {
            requestId[0] = tracker.start(null, AiFeature.RECORD_EMBEDDING);
            tracker.recordSuccess(requestId[0], 1, usage(requestId[0], "gemini-embedding-2", 500L));
            tracker.finish(requestId[0], true, 0, 15);
            status.setRollbackOnly();
        });

        AiRequest saved = requests.findByRequestId(requestId[0]).orElseThrow();
        assertThat(saved.getFinalStatus()).isEqualTo(AiRequestStatus.SUCCESS);
        assertThat(onlyAttempt(saved).getInputTokens()).isEqualTo(500L);
    }

    @Test
    void observabilityRowsNeverContainPromptOrResponseText() {
        String secret = "private-answer-marker-7921";
        UUID requestId = tracker.start(null, AiFeature.RECORD_ANALYSIS);
        RestClientResponseException exception = new RestClientResponseException(
                secret, 502, "Bad Gateway", HttpHeaders.EMPTY,
                secret.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        assertThatThrownBy(() -> observer.attempt(requestId, 1,
                () -> { throw exception; }, ignored -> null)).isSameAs(exception);
        tracker.finish(requestId, false, 0, 11);

        for (String table : List.of("ai_requests", "ai_call_attempts")) {
            List<Map<String, Object>> rows = jdbc.queryForList("select * from " + table);
            assertThat(rows).hasSize(1);
            assertThat(rows.toString()).doesNotContain(secret);
            assertThat(rows.get(0).keySet()).noneMatch(column ->
                    column.contains("prompt") || column.contains("response") || column.contains("body"));
        }
    }

    private AiUsageResponse usage(UUID requestId, String model, long inputTokens) {
        return new AiUsageResponse(requestId, "GEMINI", model, inputTokens, null, 5);
    }

    private AiCallAttempt onlyAttempt(AiRequest request) {
        return attempts.findAllByAiRequest_IdOrderByAttemptNumberAsc(request.getId()).get(0);
    }
}
