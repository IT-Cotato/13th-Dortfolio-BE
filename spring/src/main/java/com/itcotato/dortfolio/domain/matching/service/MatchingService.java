package com.itcotato.dortfolio.domain.matching.service;

import com.itcotato.dortfolio.domain.matching.config.MatchingProperties;
import com.itcotato.dortfolio.domain.matching.dto.fastapi.QuestionEmbeddingResponse;
import com.itcotato.dortfolio.domain.matching.dto.req.RecordMatchingRequest;
import com.itcotato.dortfolio.domain.matching.dto.res.MatchingRecordAnswerResponse;
import com.itcotato.dortfolio.domain.matching.dto.res.MatchingRecordResponse;
import com.itcotato.dortfolio.domain.matching.dto.res.MatchingSource;
import com.itcotato.dortfolio.domain.matching.dto.res.RecordMatchingResponse;
import com.itcotato.dortfolio.domain.matching.exception.MatchingErrorCode;
import com.itcotato.dortfolio.domain.matching.model.MatchingRecordCandidate;
import com.itcotato.dortfolio.domain.matching.repository.MatchingRecordQueryRepository;
import com.itcotato.dortfolio.domain.record.entity.RecordAnswer;
import com.itcotato.dortfolio.domain.record.repository.RecordAnswerRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingService {

	private final MatchingProperties matchingProperties;
	private final MatchingRecordQueryRepository matchingRecordQueryRepository;
	private final RecordAnswerRepository recordAnswerRepository;
	private final RecordMatchingClient recordMatchingClient;
	private final MatchingUsageLimiter matchingUsageLimiter;

	@Transactional(readOnly = true)
	public RecordMatchingResponse matchRecords(UUID userId, RecordMatchingRequest request) {
		String question = validateQuestion(request.question());
		int limit = resolveLimit(request.limit());
		matchingUsageLimiter.validateDailyLimit(userId);

		int candidateLimit = matchingProperties.candidateRecordLimit();
		List<MatchingRecordCandidate> selectedRecords = findVectorCandidates(userId, question, candidateLimit).stream()
			.limit(limit)
			.toList();
		Map<UUID, List<MatchingRecordAnswerResponse>> answersByRecordId = findAnswersByRecordId(selectedRecords);
		return RecordMatchingResponse.of(
			question,
			MatchingSource.VECTOR,
			selectedRecords.stream()
				.map(candidate -> toRecordResponse(candidate, answersByRecordId.get(candidate.recordId())))
				.toList()
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

	private List<MatchingRecordCandidate> findVectorCandidates(UUID userId, String question, int candidateLimit) {
		QuestionEmbeddingResponse response = requestQuestionEmbedding(question);
		return matchingRecordQueryRepository.findVectorCandidates(
				userId,
				response.embeddingModel(),
				response.embedding(),
				candidateLimit
			)
			.stream()
			.filter(candidate -> candidate.matchRate() >= matchingProperties.minMatchRate())
			.toList();
	}

	private QuestionEmbeddingResponse requestQuestionEmbedding(String question) {
		try {
			QuestionEmbeddingResponse response = recordMatchingClient.embedQuestion(question);
			if (response == null || response.embedding() == null || response.embedding().length == 0
				|| !StringUtils.hasText(response.embeddingModel())) {
				throw new CustomException(MatchingErrorCode.MATCHING_INVALID_AI_RESPONSE);
			}
			validateEmbedding(response.embedding());
			return response;
		} catch (RestClientException exception) {
			log.warn("Question embedding request failed.", exception);
			throw new CustomException(MatchingErrorCode.MATCHING_AI_SERVICE_UNAVAILABLE);
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

	private Map<UUID, List<MatchingRecordAnswerResponse>> findAnswersByRecordId(List<MatchingRecordCandidate> candidates) {
		Map<UUID, List<MatchingRecordAnswerResponse>> answersByRecordId = new LinkedHashMap<>();
		candidates.forEach(candidate -> answersByRecordId.put(candidate.recordId(), new ArrayList<>()));

		List<UUID> recordIds = candidates.stream()
			.map(MatchingRecordCandidate::recordId)
			.toList();
		List<RecordAnswer> answers = recordIds.isEmpty()
			? List.of()
			: recordAnswerRepository.findAllByRecordIdsOrderByRecordIdAndSortOrder(recordIds);
		answers.forEach(answer -> answersByRecordId.computeIfAbsent(answer.getRecord().getId(), key -> new ArrayList<>())
			.add(MatchingRecordAnswerResponse.from(answer)));
		return answersByRecordId;
	}

	private MatchingRecordResponse toRecordResponse(
		MatchingRecordCandidate candidate,
		List<MatchingRecordAnswerResponse> answers
	) {
		return MatchingRecordResponse.of(
			candidate.recordId(),
			candidate.recordTitle(),
			candidate.activityId(),
			candidate.activityType(),
			candidate.activityTitle(),
			candidate.templateTitle(),
			candidate.recordDate(),
			candidate.matchRate(),
			MatchingSource.VECTOR,
			candidate.summary(),
			answers
		);
	}
}
