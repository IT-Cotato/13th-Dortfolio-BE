package com.itcotato.dortfolio.domain.record.analysis.event;

import java.util.UUID;

public interface RecordAnalysisJobPublisher {

	void publish(UUID recordId);
}
