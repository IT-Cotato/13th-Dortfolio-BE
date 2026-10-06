package com.itcotato.dortfolio.domain.record.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.itcotato.dortfolio.domain.record.analysis.repository.RecordAnalysisJobRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RecordAnalysisQueueMetricsTest {
	@Test
	void exposesWaitingCountAndAgeWithZeroWhenQueueIsEmpty() {
		RecordAnalysisJobRepository repository = mock(RecordAnalysisJobRepository.class);
		Clock clock = Clock.fixed(Instant.parse("2026-10-06T06:00:00Z"), ZoneOffset.UTC);
		RecordAnalysisQueueMetrics metrics = new RecordAnalysisQueueMetrics(repository, clock);
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		metrics.bindTo(registry);
		when(repository.countWaitingJobs()).thenReturn(2L);
		when(repository.findOldestWaitingAt()).thenReturn(LocalDateTime.of(2026, 10, 6, 5, 59, 30));
		assertThat(registry.get("ai.record.queue.waiting").gauge().value()).isEqualTo(2);
		assertThat(registry.get("ai.record.queue.oldest.wait.seconds").gauge().value()).isEqualTo(30);
		when(repository.findOldestWaitingAt()).thenReturn(null);
		assertThat(registry.get("ai.record.queue.oldest.wait.seconds").gauge().value()).isZero();
	}
}
