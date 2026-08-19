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

	public void schedule(Record record) {
		if (!isAnalyzable(record)) {
			return;
		}

		RecordAnalysis analysis = recordAnalysisRepository.findByRecord_Id(record.getId())
			.orElseGet(() -> recordAnalysisRepository.save(RecordAnalysis.pending(record)));
		analysis.markPending();
		recordAnalysisJobRepository.save(RecordAnalysisJob.ready(record));
	}

	public void schedule(UUID recordId) {
		recordRepository.findById(recordId)
			.ifPresent(this::schedule);
	}

	private boolean isAnalyzable(Record record) {
		return record.getStatus() == RecordStatus.COMPLETED
			&& !record.isDeleted()
			&& !record.getActivity().isDeleted();
	}
}
