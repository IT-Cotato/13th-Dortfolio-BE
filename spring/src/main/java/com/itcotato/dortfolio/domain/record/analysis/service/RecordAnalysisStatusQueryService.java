package com.itcotato.dortfolio.domain.record.analysis.service;

import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisStatusResponse;
import com.itcotato.dortfolio.domain.record.analysis.repository.RecordAnalysisJobRepository;
import com.itcotato.dortfolio.domain.record.analysis.repository.RecordAnalysisRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.RecordErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecordAnalysisStatusQueryService {
	private final RecordRepository recordRepository;
	private final RecordAnalysisRepository analysisRepository;
	private final RecordAnalysisJobRepository jobRepository;

	public RecordAnalysisStatusResponse getStatus(UUID userId, UUID recordId) {
		recordRepository.findByIdAndUser_IdAndDeletedAtIsNull(recordId, userId)
				.filter(record -> !record.getActivity().isDeleted())
				.orElseThrow(() -> new CustomException(RecordErrorCode.RECORD_NOT_FOUND));
		return RecordAnalysisStatusResponse.of(recordId,
				analysisRepository.findByRecord_Id(recordId).orElse(null),
				jobRepository.findStatusByRecord_Id(recordId).orElse(null));
	}
}
