package com.itcotato.dortfolio.domain.insight.generation.service;

import com.itcotato.dortfolio.domain.insight.generation.lock.InsightGenerationLock;
import com.itcotato.dortfolio.domain.insight.generation.model.InsightGenerationCommand;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.InsightErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

/* Insight 생성 요청의 진입점 */
@Service
@RequiredArgsConstructor
public class InsightGenerationService {

    private final InsightGenerationLock generationLock;
    private final InsightGenerationRequestWriter requestWriter;
    private final InsightGenerationWorker worker;
    private final InsightGenerationFailureWriter failureWriter;
    private final Clock clock;

    /* Insight 생성을 요청하고 생성 ID 즉시 반환 */
    public UUID requestGeneration(UUID userId) {
        String lockToken =
                generationLock.tryAcquire(userId);

        if (lockToken == null) {
            throw new CustomException(
                    InsightErrorCode
                            .INSIGHT_GENERATION_IN_PROGRESS
            );
        }

        // Worker에 락 소유권을 넘겼는지 나타냄
        boolean handedOffToWorker = false;
        InsightGenerationCommand command = null;

        try {
            LocalDateTime snapshotAt =
                    LocalDateTime.now(clock);

            // RequestWriter는 별도 Spring Bean의 @Transactional 메서드
            command = requestWriter.createPending(
                    userId,
                    snapshotAt
            );

            try {
                // Worker는 별도 Spring Bean이므로 @Async 프록시가 적용
                worker.generate(
                        command,
                        lockToken
                );

                handedOffToWorker = true;
            } catch (TaskRejectedException exception) {
                // Executor 큐가 가득 찬 경우 @Async 메서드 본문은 실행 X
                failureWriter.fail(
                        command.insightId(),
                        "ASYNC_TASK_REJECTED",
                        "Insight 생성 작업을 실행 대기열에 등록하지 못했습니다."
                );

                throw new CustomException(
                        InsightErrorCode
                                .INSIGHT_GENERATION_FAILED
                );
            } catch (RuntimeException exception) {
                // 프록시 또는 Executor 제출 과정에서 예상하지 못한 런타임 오류가 발생한 경우에도 PENDING 정리
                failureWriter.fail(
                        command.insightId(),
                        InsightErrorCode
                                .INSIGHT_GENERATION_FAILED
                                .getCode(),
                        InsightErrorCode
                                .INSIGHT_GENERATION_FAILED
                                .getMessage()
                );

                throw new CustomException(
                        InsightErrorCode
                                .INSIGHT_GENERATION_FAILED
                );
            }

            return command.insightId();
        } finally {
            // Worker에 작업이 정상 제출되지 않은 경우에만 Service가 락을 해제
            if (!handedOffToWorker) {
                generationLock.release(
                        userId,
                        lockToken
                );
            }
        }
    }
}