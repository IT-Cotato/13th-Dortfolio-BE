package com.itcotato.dortfolio.domain.insight.generation.service;

import com.itcotato.dortfolio.domain.insight.entity.InsightGenerationStatus;
import com.itcotato.dortfolio.domain.insight.repository.InsightRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StaleInsightGenerationRecoveryScheduler {

    private final InsightRepository insightRepository;
    private final InsightGenerationFailureWriter failureWriter;
    private final Clock clock;

    @Value("${insight.generation.stale-running-timeout:10m}")
    private Duration staleRunningTimeout;

    @Scheduled(
            fixedDelayString = "${insight.generation.recovery-interval:1m}",
            initialDelayString = "${insight.generation.recovery-initial-delay:10s}"
    )
    public void recover() {
        LocalDateTime startedBefore =
                LocalDateTime.now(clock).minus(staleRunningTimeout);

        insightRepository.findAllByStatusAndStartedAtBefore(
                        InsightGenerationStatus.RUNNING,
                        startedBefore
                )
                .forEach(insight -> recover(insight.getId()));
    }

    private void recover(java.util.UUID insightId) {
        try {
            failureWriter.fail(
                    insightId,
                    "STALE_RUNNING_RECOVERED",
                    "실행 중단으로 오래 남은 Insight 생성 작업을 실패 처리했습니다."
            );
        } catch (IllegalStateException exception) {
            log.debug(
                    "Insight 상태가 이미 변경되어 복구를 건너뜁니다. insightId={}",
                    insightId
            );
        } catch (RuntimeException exception) {
            log.error(
                    "오래된 Insight RUNNING 복구에 실패했습니다. insightId={}",
                    insightId,
                    exception
            );
        }
    }
}
