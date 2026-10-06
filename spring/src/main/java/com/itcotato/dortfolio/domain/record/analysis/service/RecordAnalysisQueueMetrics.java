package com.itcotato.dortfolio.domain.record.analysis.service;

import com.itcotato.dortfolio.domain.record.analysis.repository.RecordAnalysisJobRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecordAnalysisQueueMetrics implements MeterBinder {
	private final RecordAnalysisJobRepository jobRepository;
	private final Clock clock;

	@Override
	public void bindTo(MeterRegistry registry) {
		Gauge.builder("ai.record.queue.waiting", jobRepository, RecordAnalysisJobRepository::countWaitingJobs)
				.description("Number of active record analysis jobs waiting for execution")
				.register(registry);
		Gauge.builder("ai.record.queue.oldest.wait.seconds", this, RecordAnalysisQueueMetrics::oldestWaitSeconds)
				.description("Age of the oldest current record analysis request waiting for execution")
				.register(registry);
	}

	double oldestWaitSeconds() {
		LocalDateTime oldest = jobRepository.findOldestWaitingAt();
		return oldest == null ? 0 : Math.max(0, Duration.between(oldest, LocalDateTime.now(clock)).toMillis() / 1000.0);
	}
}
