package com.itcotato.dortfolio.domain.record.analysis.event;

import com.itcotato.dortfolio.domain.record.analysis.config.RecordAnalysisProperties;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringRecordAnalysisJobPublisher implements RecordAnalysisJobPublisher {

	private final ApplicationEventPublisher eventPublisher;
	private final RecordAnalysisProperties recordAnalysisProperties;

	@Override
	public void publish(UUID recordId) {
		if (!recordAnalysisProperties.enabled()) {
			return;
		}
		eventPublisher.publishEvent(new RecordAnalysisRequestedEvent(recordId));
	}
}
