package com.itcotato.dortfolio.domain.record.analysis.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.itcotato.dortfolio.domain.record.analysis.exception.RecordAnalysisErrorCode;
import com.itcotato.dortfolio.domain.record.analysis.repository.RecordAnalysisRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

@ExtendWith(MockitoExtension.class)
class RecordAnalysisNotReadyRetryBatchRunnerTest {

	private static final String NOT_READY_CODE =
		RecordAnalysisErrorCode.STRENGTH_TAG_EMBEDDING_NOT_READY.getCode();

	@Mock
	private RecordAnalysisRepository recordAnalysisRepository;

	@Mock
	private RecordAnalysisService recordAnalysisService;

	@Test
	void retriesNotReadyFailuresAndVerifiesCompletion() {
		UUID firstRecordId = UUID.randomUUID();
		UUID secondRecordId = UUID.randomUUID();
		when(recordAnalysisRepository.findRetryableRecordIdsByFailureCode(NOT_READY_CODE))
			.thenReturn(List.of(firstRecordId, secondRecordId))
			.thenReturn(List.of());
		RecordAnalysisNotReadyRetryBatchRunner runner = runner();

		runner.run(new DefaultApplicationArguments());

		InOrder inOrder = inOrder(recordAnalysisRepository, recordAnalysisService);
		inOrder.verify(recordAnalysisRepository).findRetryableRecordIdsByFailureCode(NOT_READY_CODE);
		inOrder.verify(recordAnalysisService).analyze(firstRecordId);
		inOrder.verify(recordAnalysisService).analyze(secondRecordId);
		inOrder.verify(recordAnalysisRepository).findRetryableRecordIdsByFailureCode(NOT_READY_CODE);
	}

	@Test
	void failsBatchWhenNotReadyFailuresRemain() {
		UUID recordId = UUID.randomUUID();
		when(recordAnalysisRepository.findRetryableRecordIdsByFailureCode(NOT_READY_CODE))
			.thenReturn(List.of(recordId));
		RecordAnalysisNotReadyRetryBatchRunner runner = runner();

		assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments()))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("remaining=1");
	}

	private RecordAnalysisNotReadyRetryBatchRunner runner() {
		return new RecordAnalysisNotReadyRetryBatchRunner(
			recordAnalysisRepository,
			recordAnalysisService
		);
	}
}
