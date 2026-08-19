package com.itcotato.dortfolio.domain.record.analysis.event;

import com.itcotato.dortfolio.domain.record.analysis.config.RecordAnalysisProperties;
import com.itcotato.dortfolio.domain.record.analysis.service.RecordAnalysisJobScheduler;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringRecordAnalysisJobPublisher implements RecordAnalysisJobPublisher {

	private final RecordAnalysisProperties recordAnalysisProperties;
	private final RecordAnalysisJobScheduler jobScheduler;

	@Override
	public void publish(UUID recordId) {
		if (!recordAnalysisProperties.enabled()) {
			return;
		}
		jobScheduler.schedule(recordId);
	}
}
