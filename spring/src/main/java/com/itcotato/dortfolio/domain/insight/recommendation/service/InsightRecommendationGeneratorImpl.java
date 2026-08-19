package com.itcotato.dortfolio.domain.insight.recommendation.service;

import com.itcotato.dortfolio.domain.insight.config.InsightProperties;
import com.itcotato.dortfolio.domain.insight.recommendation.client.InsightRecommendationClient;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationCandidate;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationCompetency;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationRequest;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationResult;
import com.itcotato.dortfolio.global.exception.CustomException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import com.itcotato.dortfolio.global.exception.types.InsightErrorCode;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class InsightRecommendationGeneratorImpl
        implements InsightRecommendationGenerator {

    private final InsightRecommendationClient client;
    private final InsightProperties insightProperties;
    private final InsightRecommendationRetrySleeper retrySleeper;

    @Override
    public List<RecommendationResult> generate(
            RecommendationRequest request
    ) {
        validateRequest(request);

        FailureType lastFailure = FailureType.AI_SERVICE;

        for (int attempt = 1;
             attempt <= insightProperties.recommendationMaxAttempts();
             attempt++) {
            try {
                List<RecommendationResult> response =
                        client.generate(request);

                return validateResponse(
                        request,
                        response
                );
            } catch (InvalidRecommendationResponseException exception) {
                lastFailure = FailureType.INVALID_RESPONSE;
                break;
            } catch (RestClientResponseException exception) {
                lastFailure = FailureType.AI_SERVICE;

                if (!isRetryable(exception.getStatusCode())) {
                    break;
                }

                pauseBeforeRetry(attempt, exception);
            } catch (RestClientException exception) {
                lastFailure = FailureType.AI_SERVICE;
                pauseBeforeRetry(attempt, null);
            }
        }

        if (lastFailure == FailureType.INVALID_RESPONSE) {
            throw new CustomException(
                    InsightErrorCode
                            .INSIGHT_RECOMMENDATION_INVALID_RESPONSE
            );
        }

        throw new CustomException(
                InsightErrorCode
                        .INSIGHT_RECOMMENDATION_AI_SERVICE_FAILED
        );
    }

    private boolean isRetryable(HttpStatusCode statusCode) {
        return statusCode.value() == 429
                || statusCode.is5xxServerError();
    }

    private void pauseBeforeRetry(
            int attempt,
            RestClientResponseException exception
    ) {
        if (attempt >= insightProperties.recommendationMaxAttempts()) {
            return;
        }

        Duration backoff = calculateBackoff(attempt, exception);

        log.warn(
                "Insight recommendation request failed; retrying. "
                        + "attempt={}, maxAttempts={}, backoff={}"
                        + "{}",
                attempt,
                insightProperties.recommendationMaxAttempts(),
                backoff,
                exception == null
                        ? ""
                        : ", status=" + exception.getStatusCode().value()
        );

        retrySleeper.sleep(backoff);
    }

    private Duration calculateBackoff(
            int attempt,
            RestClientResponseException exception
    ) {
        Duration configuredBackoff = insightProperties
                .recommendationInitialBackoff()
                .multipliedBy(1L << Math.min(attempt - 1, 10));
        Duration retryAfter = parseRetryAfter(exception);
        Duration base = retryAfter.compareTo(configuredBackoff) > 0
                ? retryAfter
                : configuredBackoff;

        if (base.isZero()) {
            return base;
        }

        long jitterBound = Math.max(1L, base.toMillis() / 2L);
        long jitterMillis = ThreadLocalRandom.current()
                .nextLong(jitterBound + 1L);

        return base.plusMillis(jitterMillis);
    }

    private Duration parseRetryAfter(
            RestClientResponseException exception
    ) {
        if (exception == null || exception.getResponseHeaders() == null) {
            return Duration.ZERO;
        }

        String value = exception.getResponseHeaders()
                .getFirst(HttpHeaders.RETRY_AFTER);

        if (!StringUtils.hasText(value)) {
            return Duration.ZERO;
        }

        try {
            long seconds = Long.parseLong(value.trim());
            return seconds < 0 ? Duration.ZERO : Duration.ofSeconds(seconds);
        } catch (NumberFormatException ignored) {
            return Duration.ZERO;
        }
    }

    private void validateRequest(
            RecommendationRequest request
    ) {
        if (request == null
                || request.jobId() == null
                || !StringUtils.hasText(request.jobName())
                || request.competencies() == null
                || request.competencies().size() != 5
                || request.competencies().stream().anyMatch(
                competency -> competency == null
                        || competency.jobCompetencyId() == null
                        || !StringUtils.hasText(competency.competencyName())
                        || competency.candidates() == null
                        || competency.candidates().stream().anyMatch(
                        candidate -> candidate == null
                                || candidate.recordId() == null
                )
        ) || request.competencies().stream()
                .map(RecommendationCompetency::jobCompetencyId)
                .distinct()
                .count() != 5) {
            throw new CustomException(
                    InsightErrorCode
                            .INSIGHT_RECOMMENDATION_CANDIDATES_EMPTY
            );
        }
    }

    private List<RecommendationResult> validateResponse(
            RecommendationRequest request,
            List<RecommendationResult> response
    ) {
        if (response == null || response.size() != 5) {
            throw new InvalidRecommendationResponseException();
        }

        Map<UUID, RecommendationCompetency> competenciesById =
                request.competencies().stream().collect(Collectors.toMap(
                        RecommendationCompetency::jobCompetencyId,
                        competency -> competency
                ));
        Map<UUID, RecommendationResult> responsesById;
        try {
            responsesById = response.stream().collect(Collectors.toMap(
                    RecommendationResult::jobCompetencyId,
                    result -> result
            ));
        } catch (RuntimeException exception) {
            throw new InvalidRecommendationResponseException();
        }

        if (!responsesById.keySet().equals(competenciesById.keySet())) {
            throw new InvalidRecommendationResponseException();
        }

        return request.competencies().stream()
                .map(competency -> validateResult(
                        competency,
                        responsesById.get(competency.jobCompetencyId())
                ))
                .toList();
    }

    private RecommendationResult validateResult(
            RecommendationCompetency competency,
            RecommendationResult response
    ) {
        if (!response.matched()) {
            if (response.recordId() != null || response.reason() != null) {
                throw new InvalidRecommendationResponseException();
            }
            return new RecommendationResult(
                    false,
                    response.jobCompetencyId(),
                    null,
                    null
            );
        }

        if (response.recordId() == null
                || !StringUtils.hasText(response.reason())
                || response.reason().contains("\n")
                || response.reason().contains("\r")) {
            throw new InvalidRecommendationResponseException();
        }

        Set<UUID> candidateIds = competency.candidates()
                        .stream()
                        .map(RecommendationCandidate::recordId)
                        .collect(Collectors.toSet());

        if (!candidateIds.contains(response.recordId())) {
            throw new InvalidRecommendationResponseException();
        }

        return new RecommendationResult(
                true,
                response.jobCompetencyId(),
                response.recordId(),
                response.reason().trim()
        );
    }

    private enum FailureType {
        AI_SERVICE,
        INVALID_RESPONSE
    }

    private static final class
    InvalidRecommendationResponseException
            extends RuntimeException {
    }
}
