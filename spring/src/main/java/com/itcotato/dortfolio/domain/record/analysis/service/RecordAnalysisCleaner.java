package com.itcotato.dortfolio.domain.record.analysis.service;

import java.util.UUID;

public interface RecordAnalysisCleaner {

	void deleteByRecordId(UUID recordId);
}
