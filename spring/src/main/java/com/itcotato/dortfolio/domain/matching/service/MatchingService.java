package com.itcotato.dortfolio.domain.matching.service;

import com.itcotato.dortfolio.domain.matching.config.MatchingProperties;
import com.itcotato.dortfolio.domain.matching.dto.fastapi.QuestionEmbeddingResponse;
import com.itcotato.dortfolio.domain.matching.dto.req.RecordMatchingRequest;
import com.itcotato.dortfolio.domain.matching.dto.res.MatchingSource;
import com.itcotato.dortfolio.domain.matching.dto.res.RecordMatchingResponse;
import com.itcotato.dortfolio.domain.matching.exception.MatchingErrorCode;
import com.itcotato.dortfolio.global.exception.CustomException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingService {

	private final MatchingProperties matchingProperties;
	private final MatchingReadService matchingReadService;
	private final RecordMatchingClient recordMatchingClient;
	private final MatchingUsageLimiter matchingUsageLimiter;

	public RecordMatchingResponse matchRecords(UUID userId, RecordMatchingRequest request) {
		String question = validateQuestion(request.question());
		int limit = resolveLimit(request.limit());
		matchingUsageLimiter.validateDailyLimit(userId);

		QuestionEmbeddingResponse embeddingResponse = requestQuestionEmbedding(question);
		return RecordMatchingResponse.of(
			question,
			MatchingSource.VECTOR,
			matchingReadService.findMatchingRecords(
				userId,
				embeddingResponse.embeddingModel(),
				embeddingResponse.embedding(),
				limit
			)
		);
	}

	private String validateQuestion(String question) {
		if (!StringUtils.hasText(question)) {
			throw new CustomException(MatchingErrorCode.MATCHING_QUESTION_REQUIRED);
		}
		String trimmedQuestion = question.trim();
		if (trimmedQuestion.length() > matchingProperties.questionMaxLength()) {
			throw new CustomException(MatchingErrorCode.MATCHING_QUESTION_TOO_LONG);
		}
		return trimmedQuestion;
	}

	private int resolveLimit(Integer requestedLimit) {
		int limit = requestedLimit == null ? matchingProperties.defaultRecordLimit() : requestedLimit;
		if (limit <= 0 || limit > matchingProperties.maxRecordLimit()) {
			throw new CustomException(MatchingErrorCode.MATCHING_INVALID_LIMIT);
		}
		return limit;
	}

	private QuestionEmbeddingResponse requestQuestionEmbedding(String question) {
		try {
			QuestionEmbeddingResponse response = recordMatchingClient.embedQuestion(question);
			if (response == null || response.embedding() == null || response.embedding().length == 0
				|| !StringUtils.hasText(response.embeddingModel())) {
				throw new CustomException(MatchingErrorCode.MATCHING_INVALID_AI_RESPONSE);
			}
			validateEmbeddingModel(response.embeddingModel());
			validateEmbedding(response.embedding());
			return response;
		} catch (RestClientException exception) {
			log.warn("Question embedding request failed.", exception);
			throw new CustomException(MatchingErrorCode.MATCHING_AI_SERVICE_UNAVAILABLE);
		}
	}

	private void validateEmbeddingModel(String embeddingModel) {
		if (!matchingProperties.indexedEmbeddingModels().contains(embeddingModel)) {
			throw new CustomException(MatchingErrorCode.MATCHING_INVALID_AI_RESPONSE);
		}
	}

	private void validateEmbedding(float[] embedding) {
		if (embedding.length != matchingProperties.embeddingDimension()) {
			throw new CustomException(MatchingErrorCode.MATCHING_INVALID_AI_RESPONSE);
		}
		for (float value : embedding) {
			if (!Float.isFinite(value)) {
				throw new CustomException(MatchingErrorCode.MATCHING_INVALID_AI_RESPONSE);
			}
		}
	}
}
