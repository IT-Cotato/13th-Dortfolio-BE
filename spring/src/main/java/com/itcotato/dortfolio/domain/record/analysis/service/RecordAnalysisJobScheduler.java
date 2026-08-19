package com.itcotato.dortfolio.domain.record.analysis.service;

import com.itcotato.dortfolio.domain.record.analysis.entity.RecordAnalysis;
import com.itcotato.dortfolio.domain.record.analysis.entity.RecordAnalysisJob;
import com.itcotato.dortfolio.domain.record.analysis.repository.RecordAnalysisJobRepository;
import com.itcotato.dortfolio.domain.record.analysis.repository.RecordAnalysisRepository;
import com.itcotato.dortfolio.domain.record.entity.Record;
import com.itcotato.dortfolio.domain.record.entity.RecordStatus;
import com.itcotato.dortfolio.domain.record.repository.RecordRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecordAnalysisJobScheduler {

	private final RecordAnalysisRepository recordAnalysisRepository;
	private final RecordAnalysisJobRepository recordAnalysisJobRepository;
	private final RecordRepository recordRepository;

	public boolean schedule(Record record) {
		if (!isAnalyzable(record)) {
			return false;
		}

		RecordAnalysis analysis = recordAnalysisRepository.findByRecord_Id(record.getId())
			.orElseGet(() -> recordAnalysisRepository.save(RecordAnalysis.pending(record)));
		long generation = analysis.markPending();
		recordAnalysisJobRepository.findByRecord_Id(record.getId())
			.ifPresentOrElse(job -> job.reschedule(generation),
				() -> recordAnalysisJobRepository.save(RecordAnalysisJob.ready(record, generation)));
		return true;
	}

	public boolean schedule(UUID recordId) {
		return recordRepository.findById(recordId)
			.map(this::schedule)
			.orElse(false);
	}

	public boolean scheduleRetry(UUID recordId) {
		return recordRepository.findById(recordId)
			.filter(record -> recordAnalysisRepository.findByRecord_Id(recordId)
				.map(analysis -> analysis.getAiAnalysisStatus()
					== com.itcotato.dortfolio.domain.record.analysis.entity.AiAnalysisStatus.FAILED
					&& analysis.isFailureRetryable())
				.orElse(false))
			.map(this::schedule)
			.orElse(false);
	}

	private boolean isAnalyzable(Record record) {
		return record.getStatus() == RecordStatus.COMPLETED
			&& !record.isDeleted()
			&& !record.getActivity().isDeleted();
	}
}
