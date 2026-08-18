package com.itcotato.dortfolio.domain.record.analysis.service;

import com.itcotato.dortfolio.domain.record.analysis.repository.RecordAnalysisRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordCompetencyTagRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordEmbeddingRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordStrengthTagRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecordAnalysisDataCleaner implements RecordAnalysisCleaner {

	private final RecordAnalysisRepository recordAnalysisRepository;
	private final RecordEmbeddingRepository recordEmbeddingRepository;
	private final RecordStrengthTagRepository recordStrengthTagRepository;
	private final RecordCompetencyTagRepository legacyRecordCompetencyTagRepository;

	@Override
	public void deleteByRecordId(UUID recordId) {
		recordAnalysisRepository.deleteByRecord_Id(recordId);
		recordEmbeddingRepository.deleteAllByRecord_Id(recordId);
		recordStrengthTagRepository.deleteAllByRecord_Id(recordId);
		legacyRecordCompetencyTagRepository.deleteAllByRecord_Id(recordId);
	}
}
