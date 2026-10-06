package com.itcotato.dortfolio.domain.insight.recommendation.service;

import com.itcotato.dortfolio.domain.insight.config.InsightProperties;
import com.itcotato.dortfolio.domain.insight.recommendation.client.InsightRecommendationClient;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationCandidate;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationCompetency;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationRequest;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationResponse;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationResult;
import com.itcotato.dortfolio.domain.insight.recommendation.exception.InsightRecommendationErrorCode;
import com.itcotato.dortfolio.global.ai.retry.AiRetryExecutor;
import com.itcotato.dortfolio.global.ai.retry.AiRetryPolicy;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.ErrorCode;
import com.itcotato.dortfolio.global.exception.types.InsightErrorCode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
    private final AiRetryExecutor retryExecutor;
    private final AiRetryPolicy retryPolicy;

    @Override
    public RecommendationResponse generate(
            RecommendationRequest request
    ) {
        validateRequest(request);

        try {
            return retryExecutor.execute("insight_recommendation", () -> {
                RecommendationResponse response = client.generate(request);
                if (response == null || (response.metadata() != null && !response.metadata().isValid())) {
                    throw new InvalidRecommendationResponseException();
                }
                return RecommendationResponse.of(
                        validateResponse(request, response.recommendations()), response.metadata()
                );
            }, insightProperties.recommendationMaxAttempts(), insightProperties.recommendationInitialBackoff());
        } catch (InvalidRecommendationResponseException exception) {
            throw new CustomException(InsightErrorCode.INSIGHT_RECOMMENDATION_INVALID_RESPONSE);
        } catch (RestClientException exception) {
            ErrorCode errorCode = switch (retryPolicy.classify(exception)) {
                case TEMPORARY -> InsightErrorCode.INSIGHT_RECOMMENDATION_AI_SERVICE_FAILED;
                case INVALID_RESPONSE -> InsightErrorCode.INSIGHT_RECOMMENDATION_INVALID_RESPONSE;
                case CONFIGURATION -> InsightRecommendationErrorCode.INSIGHT_RECOMMENDATION_CONFIGURATION_ERROR;
                case OUTPUT_LIMIT -> InsightRecommendationErrorCode.INSIGHT_RECOMMENDATION_OUTPUT_LIMIT;
                case REJECTED -> InsightRecommendationErrorCode.INSIGHT_RECOMMENDATION_REJECTED;
            };
            throw new CustomException(errorCode);
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

    private static final class
    InvalidRecommendationResponseException
            extends RuntimeException {
    }
}
