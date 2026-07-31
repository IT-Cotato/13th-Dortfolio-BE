package com.itcotato.dortfolio.domain.record.analysis.event;

import com.itcotato.dortfolio.domain.record.analysis.service.RecordAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RecordAnalysisEventListener {

	private final RecordAnalysisService recordAnalysisService;

	@Async("recordAnalysisTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(RecordAnalysisRequestedEvent event) {
		recordAnalysisService.analyze(event.recordId());
	}
}
