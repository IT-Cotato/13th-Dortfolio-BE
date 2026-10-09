package com.itcotato.dortfolio.domain.record.analysis.service;

import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisRequest;
import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisResponse;

import java.util.UUID;

public interface RecordAnalysisClient {

	RecordAnalysisResponse analyze(RecordAnalysisRequest request);

	default RecordAnalysisResponse analyze(UUID requestId, RecordAnalysisRequest request) {
		return analyze(request);
	}
}
