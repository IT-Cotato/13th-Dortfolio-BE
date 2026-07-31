package com.itcotato.dortfolio.domain.record.analysis.event;

import java.util.UUID;

public record RecordAnalysisRequestedEvent(
	UUID recordId
) {
}
