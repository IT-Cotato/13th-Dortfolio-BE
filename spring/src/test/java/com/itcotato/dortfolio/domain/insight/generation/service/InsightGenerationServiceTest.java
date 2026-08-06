package com.itcotato.dortfolio.domain.insight.generation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itcotato.dortfolio.domain.insight.generation.lock.InsightGenerationLock;
import com.itcotato.dortfolio.domain.insight.generation.model.InsightGenerationCommand;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.InsightErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;

@ExtendWith(MockitoExtension.class)
class InsightGenerationServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID INSIGHT_ID = UUID.randomUUID();

    private static final Instant NOW =
            Instant.parse("2026-08-06T01:00:00Z");

    private static final ZoneId ZONE_ID =
            ZoneId.of("Asia/Seoul");

    @Mock
    private InsightGenerationLock generationLock;

    @Mock
    private InsightGenerationRequestWriter requestWriter;

    @Mock
    private InsightGenerationWorker worker;

    @Mock
    private InsightGenerationFailureWriter failureWriter;

    private InsightGenerationService service;
    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(NOW, ZONE_ID);

        service = new InsightGenerationService(
                generationLock,
                requestWriter,
                worker,
                failureWriter,
                clock
        );
    }

    @Test
    void createsPendingAndSubmitsAsyncWorker() {
        // given
        String token = "lock-token";
        LocalDateTime snapshotAt =
                LocalDateTime.now(clock);

        InsightGenerationCommand command =
                command(snapshotAt);

        when(generationLock.tryAcquire(USER_ID))
                .thenReturn(token);

        when(requestWriter.createPending(
                USER_ID,
                snapshotAt
        )).thenReturn(command);

        // when
        UUID generationId =
                service.requestGeneration(USER_ID);

        // then
        assertThat(generationId).isEqualTo(INSIGHT_ID);

        verify(requestWriter).createPending(
                USER_ID,
                snapshotAt
        );

        verify(worker).generate(
                command,
                token
        );

        verify(generationLock, never()).release(
                USER_ID,
                token
        );
    }

    @Test
    void rejectsRequestWhenLockCannotBeAcquired() {
        // given
        when(generationLock.tryAcquire(USER_ID))
                .thenReturn(null);

        // when & then
        assertThatThrownBy(
                () -> service.requestGeneration(USER_ID)
        )
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> {
                    CustomException customException =
                            (CustomException) exception;

                    assertThat(customException.getErrorCode())
                            .isEqualTo(
                                    InsightErrorCode
                                            .INSIGHT_GENERATION_IN_PROGRESS
                            );
                });

        verify(requestWriter, never())
                .createPending(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()
                );

        verify(worker, never())
                .generate(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()
                );
    }

    @Test
    void releasesLockWhenPendingCreationFails() {
        // given
        String token = "lock-token";
        LocalDateTime snapshotAt =
                LocalDateTime.now(clock);

        when(generationLock.tryAcquire(USER_ID))
                .thenReturn(token);

        when(requestWriter.createPending(
                USER_ID,
                snapshotAt
        )).thenThrow(new CustomException(
                InsightErrorCode
                        .INSIGHT_GENERATION_NOT_ALLOWED
        ));

        // when & then
        assertThatThrownBy(
                () -> service.requestGeneration(USER_ID)
        ).isInstanceOf(CustomException.class);

        verify(generationLock).release(
                USER_ID,
                token
        );

        verify(worker, never())
                .generate(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()
                );
    }

    @Test
    void marksPendingAsFailedWhenAsyncSubmissionIsRejected() {
        // given
        String token = "lock-token";
        LocalDateTime snapshotAt =
                LocalDateTime.now(clock);
        InsightGenerationCommand command =
                command(snapshotAt);

        when(generationLock.tryAcquire(USER_ID))
                .thenReturn(token);

        when(requestWriter.createPending(
                USER_ID,
                snapshotAt
        )).thenReturn(command);

        org.mockito.Mockito.doThrow(
                new TaskRejectedException(
                        "executor queue is full"
                )
        ).when(worker).generate(
                command,
                token
        );

        // when & then
        assertThatThrownBy(
                () -> service.requestGeneration(USER_ID)
        )
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> {
                    CustomException customException =
                            (CustomException) exception;

                    assertThat(customException.getErrorCode())
                            .isEqualTo(
                                    InsightErrorCode
                                            .INSIGHT_GENERATION_FAILED
                            );
                });

        verify(failureWriter).fail(
                INSIGHT_ID,
                "ASYNC_TASK_REJECTED",
                "Insight 생성 작업을 실행 대기열에 등록하지 못했습니다."
        );

        verify(generationLock).release(
                USER_ID,
                token
        );
    }

    private InsightGenerationCommand command(
            LocalDateTime snapshotAt
    ) {
        return new InsightGenerationCommand(
                INSIGHT_ID,
                USER_ID,
                UUID.randomUUID(),
                "백엔드 개발자",
                snapshotAt,
                List.of()
        );
    }
}