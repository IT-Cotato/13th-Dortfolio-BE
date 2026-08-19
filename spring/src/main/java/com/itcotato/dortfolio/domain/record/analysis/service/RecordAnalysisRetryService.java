package com.itcotato.dortfolio.domain.record.analysis.service;

import com.itcotato.dortfolio.domain.record.analysis.repository.RecordAnalysisRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecordAnalysisRetryService {

	private final RecordAnalysisRepository recordAnalysisRepository;
	private final RecordRepository recordRepository;
	private final RecordAnalysisJobScheduler jobScheduler;

	@Transactional
	public int retryFailed(UUID userId) {
		List<UUID> recordIds = recordAnalysisRepository
			.findRetryableFailedRecordIdsByUserId(userId);

		recordIds.forEach(recordId -> recordRepository.findById(recordId)
			.ifPresent(jobScheduler::schedule));

		return recordIds.size();
	}
}
