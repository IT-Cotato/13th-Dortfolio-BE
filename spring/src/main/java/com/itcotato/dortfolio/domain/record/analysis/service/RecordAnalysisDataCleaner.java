package com.itcotato.dortfolio.domain.record.analysis.service;

import com.itcotato.dortfolio.domain.record.analysis.repository.RecordAnalysisRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordCompetencyTagRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordEmbeddingRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecordAnalysisDataCleaner implements RecordAnalysisCleaner {

	private final RecordAnalysisRepository recordAnalysisRepository;
	private final RecordEmbeddingRepository recordEmbeddingRepository;
	private final RecordCompetencyTagRepository recordCompetencyTagRepository;

	@Override
	public void deleteByRecordId(UUID recordId) {
		recordAnalysisRepository.deleteByRecord_Id(recordId);
		recordEmbeddingRepository.deleteAllByRecord_Id(recordId);
		recordCompetencyTagRepository.deleteAllByRecord_Id(recordId);
	}
}
