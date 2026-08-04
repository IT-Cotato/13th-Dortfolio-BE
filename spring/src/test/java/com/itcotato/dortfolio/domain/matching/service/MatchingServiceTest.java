package com.itcotato.dortfolio.domain.matching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itcotato.dortfolio.domain.matching.config.MatchingProperties;
import com.itcotato.dortfolio.domain.matching.dto.fastapi.QuestionEmbeddingResponse;
import com.itcotato.dortfolio.domain.matching.dto.req.RecordMatchingRequest;
import com.itcotato.dortfolio.domain.matching.dto.res.MatchingSource;
import com.itcotato.dortfolio.domain.matching.exception.MatchingErrorCode;
import com.itcotato.dortfolio.global.exception.CustomException;
import java.time.Duration;
import java.time.ZoneId;
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
	private MatchingReadService matchingReadService;

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
				Duration.ofDays(1),
				ZoneId.of("Asia/Seoul"),
				List.of(new MatchingProperties.QuestionTag("TEST", "테스트 문항"))
			),
			matchingReadService,
			recordMatchingClient,
			matchingUsageLimiter
		);
	}

	@Test
	void matchRecordsDelegatesDatabaseReadsAfterCreatingQuestionEmbedding() {
		UUID userId = UUID.randomUUID();
		float[] questionEmbedding = embedding(0.1f, 0.2f);

		when(recordMatchingClient.embedQuestion(anyString()))
			.thenReturn(new QuestionEmbeddingResponse("test-embedding", questionEmbedding));
		when(matchingReadService.findMatchingRecords(eq(userId), eq("test-embedding"), same(questionEmbedding), eq(3)))
			.thenReturn(List.of());

		var response = matchingService.matchRecords(userId, new RecordMatchingRequest("목표 달성 경험", 3));

		verify(matchingUsageLimiter).validateDailyLimit(userId);
		verify(matchingReadService).findMatchingRecords(userId, "test-embedding", questionEmbedding, 3);
		assertThat(response.matchingSource()).isEqualTo(MatchingSource.VECTOR);
		assertThat(response.matchedRecords()).isEmpty();
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
		when(matchingReadService.findMatchingRecords(eq(userId), eq("test-embedding"), any(), eq(3)))
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
	void matchRecordsThrowsInvalidAiResponseWhenEmbeddingModelIsNotAllowed() {
		UUID userId = UUID.randomUUID();
		when(recordMatchingClient.embedQuestion(anyString()))
			.thenReturn(new QuestionEmbeddingResponse("unexpected-embedding", embedding(0.1f, 0.2f)));

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
	void matchRecordsPropagatesDatabaseReadFailure() {
		UUID userId = UUID.randomUUID();
		when(recordMatchingClient.embedQuestion(anyString()))
			.thenReturn(new QuestionEmbeddingResponse("test-embedding", embedding(0.1f, 0.2f)));
		when(matchingReadService.findMatchingRecords(eq(userId), eq("test-embedding"), any(), eq(3)))
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

	private float[] embedding(float first, float second) {
		float[] embedding = new float[3072];
		embedding[0] = first;
		embedding[1] = second;
		return embedding;
	}
}
