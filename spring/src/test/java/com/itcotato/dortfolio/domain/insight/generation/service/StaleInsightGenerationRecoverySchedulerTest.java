package com.itcotato.dortfolio.domain.insight.generation.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itcotato.dortfolio.domain.insight.entity.Insight;
import com.itcotato.dortfolio.domain.insight.entity.InsightGenerationStatus;
import com.itcotato.dortfolio.domain.insight.repository.InsightRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StaleInsightGenerationRecoverySchedulerTest {

    @Mock
    private InsightRepository insightRepository;

    @Mock
    private InsightGenerationFailureWriter failureWriter;

    @Mock
    private Insight runningInsight;

    @Test
    void recoversOnlyRunningInsightsOlderThanStartedTimeout() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-08T08:00:00Z"),
                ZoneOffset.UTC
        );
        StaleInsightGenerationRecoveryScheduler scheduler =
                new StaleInsightGenerationRecoveryScheduler(
                        insightRepository,
                        failureWriter,
                        clock
                );
        ReflectionTestUtils.setField(
                scheduler,
                "staleRunningTimeout",
                Duration.ofMinutes(10)
        );
        LocalDateTime startedBefore =
                LocalDateTime.of(2026, 8, 8, 7, 50);
        UUID insightId = UUID.randomUUID();

        when(runningInsight.getId()).thenReturn(insightId);
        when(insightRepository.findAllByStatusAndStartedAtBefore(
                InsightGenerationStatus.RUNNING,
                startedBefore
        )).thenReturn(List.of(runningInsight));

        scheduler.recover();

        verify(failureWriter).fail(
                insightId,
                "STALE_RUNNING_RECOVERED",
                "실행 중단으로 오래 남은 Insight 생성 작업을 실패 처리했습니다."
        );
    }
}
