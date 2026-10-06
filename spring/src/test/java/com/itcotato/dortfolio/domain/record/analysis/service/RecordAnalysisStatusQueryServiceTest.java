package com.itcotato.dortfolio.domain.record.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.itcotato.dortfolio.domain.activity.entity.Activity;
import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisStatusResponse;
import com.itcotato.dortfolio.domain.record.analysis.entity.AiAnalysisStatus;
import com.itcotato.dortfolio.domain.record.analysis.entity.RecordAnalysis;
import com.itcotato.dortfolio.domain.record.analysis.entity.RecordAnalysisJob;
import com.itcotato.dortfolio.domain.record.analysis.repository.RecordAnalysisJobRepository;
import com.itcotato.dortfolio.domain.record.analysis.repository.RecordAnalysisRepository;
import com.itcotato.dortfolio.domain.record.entity.Record;
import com.itcotato.dortfolio.domain.record.repository.RecordRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.RecordErrorCode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecordAnalysisStatusQueryServiceTest {
	private final RecordRepository records = mock(RecordRepository.class);
	private final RecordAnalysisRepository analyses = mock(RecordAnalysisRepository.class);
	private final RecordAnalysisJobRepository jobs = mock(RecordAnalysisJobRepository.class);
	private final RecordAnalysisStatusQueryService service = new RecordAnalysisStatusQueryService(records, analyses, jobs);

	@Test
	void checksOwnershipAndDeletionBeforeReadingAnalysis() {
		UUID userId = UUID.randomUUID();
		UUID recordId = UUID.randomUUID();
		when(records.findByIdAndUser_IdAndDeletedAtIsNull(recordId, userId)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.getStatus(userId, recordId)).isInstanceOf(CustomException.class)
				.extracting(exception -> ((CustomException) exception).getErrorCode()).isEqualTo(RecordErrorCode.RECORD_NOT_FOUND);
		verifyNoInteractions(analyses, jobs);
	}

	@Test
	void hidesRecordsWhoseActivityIsDeleted() {
		UUID userId = UUID.randomUUID();
		UUID recordId = UUID.randomUUID();
		Record record = mock(Record.class);
		Activity activity = mock(Activity.class);
		when(records.findByIdAndUser_IdAndDeletedAtIsNull(recordId, userId)).thenReturn(Optional.of(record));
		when(record.getActivity()).thenReturn(activity);
		when(activity.isDeleted()).thenReturn(true);
		assertThatThrownBy(() -> service.getStatus(userId, recordId)).isInstanceOf(CustomException.class);
		verifyNoInteractions(analyses, jobs);
	}

	@Test
	void reportsWorkerStateOnlyForCurrentGeneration() {
		RecordAnalysis analysis = RecordAnalysis.pending(null);
		long generation = analysis.markPending();
		RecordAnalysisJob job = RecordAnalysisJob.ready(null, generation);
		LocalDateTime now = LocalDateTime.now();
		job.start(now, now.plusMinutes(1));
		UUID recordId = UUID.randomUUID();
		assertThat(RecordAnalysisStatusResponse.of(recordId, analysis, job).status()).isEqualTo("RUNNING");
		analysis.markPending();
		assertThat(RecordAnalysisStatusResponse.of(recordId, analysis, job).status()).isEqualTo("PENDING");
		analysis.fail("RA001 일시적인 오류", true);
		RecordAnalysisStatusResponse response = RecordAnalysisStatusResponse.of(recordId, analysis, job);
		assertThat(response.status()).isEqualTo("FAILED");
		assertThat(response.failureCode()).isEqualTo("RA001");
		assertThat(response.failureRetryable()).isTrue();
		assertThat(RecordAnalysisStatusResponse.of(recordId, null, null).status()).isEqualTo("NOT_REQUESTED");
	}
}
