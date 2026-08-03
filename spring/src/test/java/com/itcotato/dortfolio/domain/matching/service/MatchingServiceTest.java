package com.itcotato.dortfolio.domain.matching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.itcotato.dortfolio.domain.matching.config.MatchingProperties;
import com.itcotato.dortfolio.domain.matching.dto.fastapi.QuestionEmbeddingResponse;
import com.itcotato.dortfolio.domain.matching.dto.req.RecordMatchingRequest;
import com.itcotato.dortfolio.domain.matching.dto.res.MatchingSource;
import com.itcotato.dortfolio.domain.matching.exception.MatchingErrorCode;
import com.itcotato.dortfolio.domain.matching.model.MatchingRecordCandidate;
import com.itcotato.dortfolio.domain.matching.repository.MatchingRecordQueryRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordAnswerRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

	@Mock
	private MatchingRecordQueryRepository matchingRecordQueryRepository;

	@Mock
	private RecordAnswerRepository recordAnswerRepository;

	@Mock
	private RecordMatchingClient recordMatchingClient;

	@Mock
	private MatchingUsageLimiter matchingUsageLimiter;

	private MatchingService matchingService;

	@BeforeEach
	void setUp() {
		matchingService = new MatchingService(
			new MatchingProperties(
				3,
				10,
				10,
				3072,
				List.of("test-embedding"),
				500,
				30,
				20,
				java.time.Duration.ofDays(1),
				List.of(new MatchingProperties.QuestionTag("TEST", "테스트 문항"))
			),
			matchingRecordQueryRepository,
			recordAnswerRepository,
			recordMatchingClient,
			matchingUsageLimiter
		);
	}

	@Test
	void matchRecordsReturnsTopVectorCandidatesWithoutGenerationMatching() {
		UUID userId = UUID.randomUUID();
		MatchingRecordCandidate first = candidate(UUID.randomUUID(), UUID.randomUUID(), "첫 번째 기록", 0.91);
		MatchingRecordCandidate second = candidate(UUID.randomUUID(), UUID.randomUUID(), "두 번째 기록", 0.82);
		MatchingRecordCandidate third = candidate(UUID.randomUUID(), UUID.randomUUID(), "세 번째 기록", 0.76);

		when(recordMatchingClient.embedQuestion(anyString()))
			.thenReturn(new QuestionEmbeddingResponse("test-embedding", embedding(0.1f, 0.2f)));
		when(matchingRecordQueryRepository.findVectorCandidates(eq(userId), eq("test-embedding"), any(), anyInt()))
			.thenReturn(List.of(first, second, third));
		when(recordAnswerRepository.findAllByRecordIdsOrderByRecordIdAndSortOrder(any()))
			.thenReturn(List.of());

		var response = matchingService.matchRecords(userId, new RecordMatchingRequest("목표 달성 경험", 3));

		List<UUID> recordIds = response.matchedRecords().stream()
			.map(record -> record.recordId())
			.toList();
		assertThat(recordIds).containsExactly(first.recordId(), second.recordId(), third.recordId());
		assertThat(response.matchingSource()).isEqualTo(MatchingSource.VECTOR);
		assertThat(response.matchedRecords().get(0).matchingSource()).isEqualTo(MatchingSource.VECTOR);
		assertThat(response.matchedRecords().get(0).activityId()).isEqualTo(first.activityId());
	}

	@Test
	void matchRecordsThrowsServiceUnavailableWhenQuestionEmbeddingFails() {
		UUID userId = UUID.randomUUID();
		when(recordMatchingClient.embedQuestion(anyString()))
			.thenThrow(new RestClientException("timeout"));

		assertThatThrownBy(() -> matchingService.matchRecords(userId, new RecordMatchingRequest("문제 해결", null)))
			.isInstanceOf(CustomException.class)
			.extracting(exception -> ((CustomException) exception).getErrorCode())
			.isEqualTo(MatchingErrorCode.MATCHING_AI_SERVICE_UNAVAILABLE);
	}

	@Test
	void matchRecordsReturnsEmptyWhenVectorCandidatesAreEmpty() {
		UUID userId = UUID.randomUUID();
		when(recordMatchingClient.embedQuestion(anyString()))
			.thenReturn(new QuestionEmbeddingResponse("test-embedding", embedding(0.1f, 0.2f)));
		when(matchingRecordQueryRepository.findVectorCandidates(eq(userId), eq("test-embedding"), any(), anyInt()))
			.thenReturn(List.of());

		var response = matchingService.matchRecords(userId, new RecordMatchingRequest("문제 해결", 3));

		assertThat(response.matchedRecords()).isEmpty();
		assertThat(response.matchingSource()).isEqualTo(MatchingSource.VECTOR);
	}

	@Test
	void matchRecordsRejectsBlankQuestion() {
		assertThatThrownBy(() -> matchingService.matchRecords(UUID.randomUUID(), new RecordMatchingRequest(" ", 3)))
			.isInstanceOf(CustomException.class)
			.extracting(exception -> ((CustomException) exception).getErrorCode())
			.isEqualTo(MatchingErrorCode.MATCHING_QUESTION_REQUIRED);
	}

	@Test
	void matchRecordsThrowsInvalidAiResponseWhenQuestionEmbeddingDimensionIsInvalid() {
		UUID userId = UUID.randomUUID();
		when(recordMatchingClient.embedQuestion(anyString()))
			.thenReturn(new QuestionEmbeddingResponse("test-embedding", new float[] {0.1f, 0.2f}));

		assertThatThrownBy(() -> matchingService.matchRecords(userId, new RecordMatchingRequest("문제 해결", null)))
			.isInstanceOf(CustomException.class)
			.extracting(exception -> ((CustomException) exception).getErrorCode())
			.isEqualTo(MatchingErrorCode.MATCHING_INVALID_AI_RESPONSE);
	}

	@Test
	void matchRecordsThrowsInvalidAiResponseWhenQuestionEmbeddingContainsNonFiniteValue() {
		UUID userId = UUID.randomUUID();
		float[] embedding = embedding(0.1f, 0.2f);
		embedding[2] = Float.NaN;
		when(recordMatchingClient.embedQuestion(anyString()))
			.thenReturn(new QuestionEmbeddingResponse("test-embedding", embedding));

		assertThatThrownBy(() -> matchingService.matchRecords(userId, new RecordMatchingRequest("문제 해결", null)))
			.isInstanceOf(CustomException.class)
			.extracting(exception -> ((CustomException) exception).getErrorCode())
			.isEqualTo(MatchingErrorCode.MATCHING_INVALID_AI_RESPONSE);
	}

	@Test
	void matchRecordsRejectsInvalidLimit() {
		assertThatThrownBy(() -> matchingService.matchRecords(UUID.randomUUID(), new RecordMatchingRequest("질문", 11)))
			.isInstanceOf(CustomException.class)
			.extracting(exception -> ((CustomException) exception).getErrorCode())
			.isEqualTo(MatchingErrorCode.MATCHING_INVALID_LIMIT);
	}

	@Test
	void matchRecordsPropagatesVectorCandidateQueryFailure() {
		UUID userId = UUID.randomUUID();
		when(recordMatchingClient.embedQuestion(anyString()))
			.thenReturn(new QuestionEmbeddingResponse("test-embedding", embedding(0.1f, 0.2f)));
		when(matchingRecordQueryRepository.findVectorCandidates(eq(userId), eq("test-embedding"), any(), anyInt()))
			.thenThrow(new IllegalStateException("database error"));

		assertThatThrownBy(() -> matchingService.matchRecords(userId, new RecordMatchingRequest("질문", 3)))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("database error");
	}

	@Test
	void matchRecordsRejectsWhenDailyLimitIsExceeded() {
		UUID userId = UUID.randomUUID();
		org.mockito.Mockito.doThrow(new CustomException(MatchingErrorCode.MATCHING_DAILY_LIMIT_EXCEEDED))
			.when(matchingUsageLimiter)
			.validateDailyLimit(userId);

		assertThatThrownBy(() -> matchingService.matchRecords(userId, new RecordMatchingRequest("질문", 3)))
			.isInstanceOf(CustomException.class)
			.extracting(exception -> ((CustomException) exception).getErrorCode())
			.isEqualTo(MatchingErrorCode.MATCHING_DAILY_LIMIT_EXCEEDED);
	}

	private MatchingRecordCandidate candidate(UUID recordId, UUID activityId, String title, double score) {
		return new MatchingRecordCandidate(
			recordId,
			title,
			LocalDateTime.of(2026, 7, 20, 15, 30),
			activityId,
			"동아리",
			"Dortfolio",
			LocalDate.of(2026, 3, 1),
			null,
			true,
			"문제 해결 기록",
			title + " 요약",
			score
		);
	}

	private float[] embedding(float first, float second) {
		float[] embedding = new float[3072];
		embedding[0] = first;
		embedding[1] = second;
		return embedding;
	}
}
