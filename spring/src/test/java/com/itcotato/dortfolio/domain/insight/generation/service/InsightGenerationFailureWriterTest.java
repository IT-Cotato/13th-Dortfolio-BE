package com.itcotato.dortfolio.domain.insight.generation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.itcotato.dortfolio.domain.insight.entity.Insight;
import com.itcotato.dortfolio.domain.insight.entity.InsightGenerationStatus;
import com.itcotato.dortfolio.domain.insight.repository.InsightRepository;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class InsightGenerationFailureWriterTest {

    private static final LocalDateTime SNAPSHOT_AT =
            LocalDateTime.of(2026, 8, 1, 10, 0);

    @Autowired
    private InsightGenerationFailureWriter failureWriter;

    @Autowired
    private InsightRepository insightRepository;

    @Autowired
    private UserRepository userRepository;

    private UUID createdUserId;
    private UUID createdInsightId;

    @AfterEach
    void tearDown() {
        if (createdInsightId != null) {
            insightRepository.deleteById(createdInsightId);
        }
        if (createdUserId != null) {
            userRepository.deleteById(createdUserId);
        }
    }

    @Test
    void marksPendingInsightAsFailed() {
        // given
        Insight insight = createPendingInsight();

        // when
        failureWriter.fail(
                insight.getId(),
                "AI_TIMEOUT",
                "Gemini 요청 시간이 초과되었습니다."
        );

        // then
        Insight savedInsight = insightRepository.findById(insight.getId())
                .orElseThrow();

        assertThat(savedInsight.getStatus())
                .isEqualTo(InsightGenerationStatus.FAILED);
        assertThat(savedInsight.getFailedAt()).isNotNull();
        assertThat(savedInsight.getCompletedAt()).isNull();
        assertThat(savedInsight.getFailureCode())
                .isEqualTo("AI_TIMEOUT");
        assertThat(savedInsight.getFailureMessage())
                .isEqualTo("Gemini 요청 시간이 초과되었습니다.");
    }

    @Test
    void usesFallbackWhenFailureInformationIsBlank() {
        // given
        Insight insight = createPendingInsight();

        // when
        failureWriter.fail(
                insight.getId(),
                " ",
                null
        );

        // then
        Insight savedInsight = insightRepository.findById(insight.getId())
                .orElseThrow();

        assertThat(savedInsight.getFailureCode())
                .isEqualTo("UNKNOWN_ERROR");
        assertThat(savedInsight.getFailureMessage())
                .isEqualTo("Insight generation failed.");
    }

    @Test
    void completedInsightCannotBeChangedToFailed() {
        // given
        Insight insight = createPendingInsight();
        insight.complete(SNAPSHOT_AT.plusMinutes(1));
        insightRepository.saveAndFlush(insight);

        // when & then
        assertThatThrownBy(() -> failureWriter.fail(
                insight.getId(),
                "AI_ERROR",
                "완료 이후 발생한 오류"
        )).isInstanceOf(IllegalStateException.class);

        Insight savedInsight = insightRepository.findById(insight.getId())
                .orElseThrow();

        assertThat(savedInsight.getStatus())
                .isEqualTo(InsightGenerationStatus.COMPLETED);
        assertThat(savedInsight.getFailureCode()).isNull();
    }

    @Test
    void failedInsightCannotBeFailedAgain() {
        // given
        Insight insight = createPendingInsight();
        insight.fail(
                SNAPSHOT_AT.plusMinutes(1),
                "FIRST_ERROR",
                "최초 실패"
        );
        insightRepository.saveAndFlush(insight);

        // when & then
        assertThatThrownBy(() -> failureWriter.fail(
                insight.getId(),
                "SECOND_ERROR",
                "두 번째 실패"
        )).isInstanceOf(IllegalStateException.class);

        Insight savedInsight = insightRepository.findById(insight.getId())
                .orElseThrow();

        assertThat(savedInsight.getFailureCode())
                .isEqualTo("FIRST_ERROR");
        assertThat(savedInsight.getFailureMessage())
                .isEqualTo("최초 실패");
    }

    private Insight createPendingInsight() {
        User user = userRepository.save(
                User.of(
                        UUID.randomUUID() + "@test.com",
                        "encoded-password",
                        "인사이트 테스트 사용자"
                )
        );
        createdUserId = user.getId();

        Insight insight = Insight.pending(
                user,
                UUID.randomUUID(),
                "백엔드 개발자",
                SNAPSHOT_AT,
                10,
                SNAPSHOT_AT
        );

        Insight savedInsight = insightRepository.saveAndFlush(insight);
        createdInsightId = savedInsight.getId();
        return savedInsight;
    }
}
