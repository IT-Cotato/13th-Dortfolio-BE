package com.itcotato.dortfolio.domain.insight.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.itcotato.dortfolio.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InsightTest {

    private static final LocalDateTime REQUESTED_AT = LocalDateTime.of(2026, 8, 1, 10, 0);

    @Test
    void completesPendingInsight() {
        Insight insight = createPendingInsight();
        LocalDateTime completedAt = REQUESTED_AT.plusMinutes(1);

        insight.complete(completedAt);

        assertThat(insight.getStatus()).isEqualTo(InsightGenerationStatus.COMPLETED);
        assertThat(insight.getCompletedAt()).isEqualTo(completedAt);
        assertThat(insight.getFailedAt()).isNull();
    }

    @Test
    void storesFailureInformation() {
        Insight insight = createPendingInsight();
        LocalDateTime failedAt = REQUESTED_AT.plusMinutes(1);

        insight.fail(failedAt, "AI_TIMEOUT", "AI server did not respond");

        assertThat(insight.getStatus()).isEqualTo(InsightGenerationStatus.FAILED);
        assertThat(insight.getFailedAt()).isEqualTo(failedAt);
        assertThat(insight.getFailureCode()).isEqualTo("AI_TIMEOUT");
        assertThat(insight.getFailureMessage()).isEqualTo("AI server did not respond");
    }

    @Test
    void completedInsightCannotChangeStatusAgain() {
        Insight insight = createPendingInsight();
        insight.complete(REQUESTED_AT.plusMinutes(1));

        assertThatThrownBy(() -> insight.fail(
                REQUESTED_AT.plusMinutes(2),
                "AI_ERROR",
                "retry failed"
        )).isInstanceOf(IllegalStateException.class);
    }

    private Insight createPendingInsight() {
        return Insight.pending(
                User.of("insight@test.com", "encoded-password", "인사이트 테스트"),
                UUID.randomUUID(),
                "백엔드 개발자",
                REQUESTED_AT,
                10,
                REQUESTED_AT
        );
    }
}
