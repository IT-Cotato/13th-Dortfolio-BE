package com.itcotato.dortfolio.domain.insight.recommendation.service;

import com.itcotato.dortfolio.domain.insight.config.InsightProperties;
import com.itcotato.dortfolio.domain.insight.recommendation.client.InsightRecommendationClient;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationCandidate;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationRequest;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationResult;
import com.itcotato.dortfolio.global.exception.CustomException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import com.itcotato.dortfolio.global.exception.types.InsightErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class InsightRecommendationGeneratorImpl
        implements InsightRecommendationGenerator {

    private final InsightRecommendationClient client;
    private final InsightProperties insightProperties;

    @Override
    public RecommendationResult generate(
            RecommendationRequest request
    ) {
        validateRequest(request);

        FailureType lastFailure = FailureType.AI_SERVICE;

        for (int attempt = 1;
             attempt <= insightProperties.recommendationMaxAttempts();
             attempt++) {
            try {
                RecommendationResult response =
                        client.generate(request);

                return validateResponse(
                        request,
                        response
                );
            } catch (InvalidRecommendationResponseException exception) {
                lastFailure = FailureType.INVALID_RESPONSE;
            } catch (RestClientException exception) {
                lastFailure = FailureType.AI_SERVICE;
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

    private void validateRequest(
            RecommendationRequest request
    ) {
        if (request == null
                || request.jobId() == null
                || !StringUtils.hasText(request.jobName())
                || request.jobCompetencyId() == null
                || !StringUtils.hasText(request.competencyName())
                || request.candidates() == null
                || request.candidates().isEmpty()
                || request.candidates().stream().anyMatch(
                candidate -> candidate == null
                        || candidate.recordId() == null
        )) {
            throw new CustomException(
                    InsightErrorCode
                            .INSIGHT_RECOMMENDATION_CANDIDATES_EMPTY
            );
        }
    }

    private RecommendationResult validateResponse(
            RecommendationRequest request,
            RecommendationResult response
    ) {
        if (response == null
                || response.jobCompetencyId() == null
                || response.recordId() == null
                || !StringUtils.hasText(response.reason())
                || response.reason().contains("\n")
                || response.reason().contains("\r")) {
            throw new InvalidRecommendationResponseException();
        }

        if (!request.jobCompetencyId().equals(
                response.jobCompetencyId()
        )) {
            throw new InvalidRecommendationResponseException();
        }

        Set<UUID> candidateIds =
                request.candidates()
                        .stream()
                        .map(RecommendationCandidate::recordId)
                        .collect(Collectors.toSet());

        if (!candidateIds.contains(response.recordId())) {
            throw new InvalidRecommendationResponseException();
        }

        return new RecommendationResult(
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
