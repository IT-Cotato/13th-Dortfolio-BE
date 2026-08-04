package com.itcotato.dortfolio.domain.matching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itcotato.dortfolio.domain.matching.config.MatchingProperties;
import com.itcotato.dortfolio.domain.matching.model.MatchingRecordCandidate;
import com.itcotato.dortfolio.domain.matching.repository.MatchingRecordQueryRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordAnswerRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchingReadServiceTest {

	@Mock
	private MatchingRecordQueryRepository matchingRecordQueryRepository;

	@Mock
	private RecordAnswerRepository recordAnswerRepository;

	private MatchingReadService matchingReadService;

	@BeforeEach
	void setUp() {
		matchingReadService = new MatchingReadService(
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
			matchingRecordQueryRepository,
			recordAnswerRepository
		);
	}

	@Test
	void findMatchingRecordsFiltersByMinimumRateAndRequestedLimit() {
		UUID userId = UUID.randomUUID();
		float[] embedding = new float[3072];
		MatchingRecordCandidate first = candidate("첫 번째", 0.91);
		MatchingRecordCandidate second = candidate("두 번째", 0.82);
		MatchingRecordCandidate third = candidate("세 번째", 0.76);
		MatchingRecordCandidate belowMinimum = candidate("최소 점수 미만", 0.20);
		when(matchingRecordQueryRepository.findVectorCandidates(userId, "test-embedding", embedding, 10))
			.thenReturn(List.of(first, second, third, belowMinimum));
		when(recordAnswerRepository.findAllByRecordIdsOrderByRecordIdAndSortOrder(any()))
			.thenReturn(List.of());

		var result = matchingReadService.findMatchingRecords(userId, "test-embedding", embedding, 2);

		assertThat(result)
			.extracting(record -> record.recordId())
			.containsExactly(first.recordId(), second.recordId());
		verify(recordAnswerRepository).findAllByRecordIdsOrderByRecordIdAndSortOrder(
			eq(List.of(first.recordId(), second.recordId()))
		);
	}

	@Test
	void findMatchingRecordsSkipsAnswerQueryWhenNoCandidateExists() {
		UUID userId = UUID.randomUUID();
		float[] embedding = new float[3072];
		when(matchingRecordQueryRepository.findVectorCandidates(userId, "test-embedding", embedding, 10))
			.thenReturn(List.of());

		var result = matchingReadService.findMatchingRecords(userId, "test-embedding", embedding, 3);

		assertThat(result).isEmpty();
		verify(recordAnswerRepository, never()).findAllByRecordIdsOrderByRecordIdAndSortOrder(any());
	}

	private MatchingRecordCandidate candidate(String title, double score) {
		return new MatchingRecordCandidate(
			UUID.randomUUID(),
			title,
			LocalDateTime.of(2026, 7, 20, 15, 30),
			UUID.randomUUID(),
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
}
