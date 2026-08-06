package com.itcotato.dortfolio.domain.insight.generation.service;

import com.itcotato.dortfolio.domain.insight.generation.lock.InsightGenerationLock;
import com.itcotato.dortfolio.domain.insight.generation.model.InsightGenerationCommand;
import com.itcotato.dortfolio.domain.insight.generation.model.InsightGenerationStartResult;
import com.itcotato.dortfolio.domain.insight.repository.InsightRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.InsightErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InsightGenerationService {

    private final InsightGenerationLock generationLock;
    private final InsightGenerationRequestWriter requestWriter;
    private final InsightGenerationWorker worker;
    private final InsightGenerationFailureWriter failureWriter;
    private final InsightRepository insightRepository;
    private final Clock clock;

    /* Insight 생성을 요청 */
    public InsightGenerationStartResult requestGeneration(
            UUID userId
    ) {
        String lockToken =
                generationLock.tryAcquire(userId);

        if (lockToken == null) {

            return findPendingOrThrow(userId);
        }

        boolean handedOffToWorker = false;

        try {
            LocalDateTime snapshotAt =
                    LocalDateTime.now(clock);

            InsightGenerationCommand command;

            try {
                command = requestWriter.createPending(
                        userId,
                        snapshotAt
                );
            } catch (CustomException exception) {
                // Redis 락이 만료된 뒤 다른 요청이 락을 얻었지만 기존 Worker의 PENDING이 남아 있는 경우
                if (exception.getErrorCode()
                        == InsightErrorCode
                        .INSIGHT_GENERATION_IN_PROGRESS) {
                    return findPendingOrThrow(userId);
                }

                throw exception;
            }

            try {
                worker.generate(
                        command,
                        lockToken
                );

                handedOffToWorker = true;
            } catch (TaskRejectedException exception) {
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

            return InsightGenerationStartResult.pending(
                    command.insightId()
            );
        } finally {
            if (!handedOffToWorker) {
                generationLock.release(
                        userId,
                        lockToken
                );
            }
        }
    }

    private InsightGenerationStartResult findPendingOrThrow(
            UUID userId
    ) {
        return insightRepository.findPendingByUserId(userId)
                .map(insight ->
                        InsightGenerationStartResult.pending(
                                insight.getId()
                        )
                )
                .orElseThrow(() -> new CustomException(
                        InsightErrorCode
                                .INSIGHT_GENERATION_IN_PROGRESS
                ));
    }
}