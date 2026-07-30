package com.itcotato.dortfolio.domain.record.analysis.service;

import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisRequest;
import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisResponse;

public interface RecordAnalysisClient {

	RecordAnalysisResponse analyze(RecordAnalysisRequest request);
}
