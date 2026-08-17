package com.itcotato.dortfolio.domain.insight.generation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.itcotato.dortfolio.domain.insight.entity.Insight;
import com.itcotato.dortfolio.domain.insight.entity.InsightGenerationStatus;
import com.itcotato.dortfolio.domain.insight.entity.InsightJobRecommendationMatchStatus;
import com.itcotato.dortfolio.domain.insight.generation.model.InsightGenerationResult;
import com.itcotato.dortfolio.domain.insight.generation.model.InsightGenerationResult.JobRecommendationResult;
import com.itcotato.dortfolio.domain.insight.generation.model.InsightGenerationResult.StrengthRecordResult;
import com.itcotato.dortfolio.domain.insight.generation.model.InsightGenerationResult.StrengthResult;
import com.itcotato.dortfolio.domain.insight.generation.model.InsightGenerationResult.TemplateResult;
import com.itcotato.dortfolio.domain.insight.repository.InsightJobRecommendationRepository;
import com.itcotato.dortfolio.domain.insight.repository.InsightRepository;
import com.itcotato.dortfolio.domain.insight.repository.InsightStrengthRecordRepository;
import com.itcotato.dortfolio.domain.insight.repository.InsightStrengthRepository;
import com.itcotato.dortfolio.domain.insight.repository.InsightTemplateStatisticRepository;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import jakarta.persistence.PersistenceException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class InsightGenerationResultWriterTest {

    private static final LocalDateTime SNAPSHOT_AT =
            LocalDateTime.of(2026, 8, 1, 10, 0);

    @Autowired
    private InsightGenerationResultWriter resultWriter;

    @Autowired
    private InsightRepository insightRepository;

    @Autowired
    private InsightStrengthRepository strengthRepository;

    @Autowired
    private InsightStrengthRecordRepository strengthRecordRepository;

    @Autowired
    private InsightTemplateStatisticRepository templateRepository;

    @Autowired
    private InsightJobRecommendationRepository recommendationRepository;

    @Autowired
    private UserRepository userRepository;

    private UUID createdUserId;
    private UUID createdInsightId;

    @AfterEach
    void tearDown() {
        if (createdInsightId != null) {
            var strengths = strengthRepository
                    .findAllByInsight_IdOrderByRankAsc(createdInsightId);

            strengths.forEach(strength ->
                    strengthRecordRepository.deleteAll(
                            strengthRecordRepository
                                    .findAllByInsightStrength_Id(strength.getId())
                    )
            );

            recommendationRepository.deleteAll(
                    recommendationRepository.findAllByInsight_Id(createdInsightId)
            );
            templateRepository.deleteAll(
                    templateRepository
                            .findAllByInsight_IdOrderByRankAsc(createdInsightId)
            );
            strengthRepository.deleteAll(strengths);
            insightRepository.deleteById(createdInsightId);
        }

        if (createdUserId != null) {
            userRepository.deleteById(createdUserId);
        }
    }

    @Test
    void savesAllResultsAndCompletesInsight() {
        // given
        Insight insight = createPendingInsight();

        UUID strengthTagId = UUID.randomUUID();
        UUID strengthRecordId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID competencyId = UUID.randomUUID();
        UUID recommendationRecordId = UUID.randomUUID();

        InsightGenerationResult result =
                new InsightGenerationResult(
                        insight.getId(),
                        List.of(
                                new StrengthResult(
                                        strengthTagId,
                                        "문제 해결",
                                        6,
                                        0.91,
                                        0.6,
                                        1,
                                        List.of(
                                                new StrengthRecordResult(
                                                        strengthRecordId,
                                                        "API 성능 개선",
                                                        SNAPSHOT_AT.minusDays(1)
                                                )
                                        )
                                )
                        ),
                        List.of(
                                new TemplateResult(
                                        templateId,
                                        "문제 해결 경험",
                                        6,
                                        0.6,
                                        1
                                )
                        ),
                        List.of(
                                new JobRecommendationResult(
                                        competencyId,
                                        "문제 해결",
                                        recommendationRecordId,
                                        "API 응답 속도 개선",
                                        "문제 해결 경험",
                                        "병목을 분석하고 개선한 과정이 구체적입니다.",
                                        0.94
                                )
                        )
                );

        // when
        resultWriter.complete(result);

        // then
        Insight savedInsight = insightRepository.findById(insight.getId())
                .orElseThrow();

        assertThat(savedInsight.getStatus())
                .isEqualTo(InsightGenerationStatus.COMPLETED);
        assertThat(savedInsight.getCompletedAt()).isNotNull();
        assertThat(savedInsight.getFailedAt()).isNull();
        assertThat(savedInsight.getFailureCode()).isNull();
        assertThat(savedInsight.getFailureMessage()).isNull();

        var strengths = strengthRepository
                .findAllByInsight_IdOrderByRankAsc(insight.getId());

        assertThat(strengths).hasSize(1);
        assertThat(strengths.get(0).getStrengthTagIdSnapshot())
                .isEqualTo(strengthTagId);
        assertThat(strengths.get(0).getStrengthNameSnapshot())
                .isEqualTo("문제 해결");
        assertThat(strengths.get(0).getRecordCount())
                .isEqualTo(6);
        assertThat(strengths.get(0).getRank())
                .isEqualTo(1);

        var strengthRecords = strengthRecordRepository
                .findAllByInsightStrength_Id(strengths.get(0).getId());

        assertThat(strengthRecords).hasSize(1);
        assertThat(strengthRecords.get(0).getRecordIdSnapshot())
                .isEqualTo(strengthRecordId);
        assertThat(strengthRecords.get(0).getRecordTitleSnapshot())
                .isEqualTo("API 성능 개선");

        var templates = templateRepository
                .findAllByInsight_IdOrderByRankAsc(insight.getId());

        assertThat(templates).hasSize(1);
        assertThat(templates.get(0).getTemplateIdSnapshot())
                .isEqualTo(templateId);
        assertThat(templates.get(0).getTemplateNameSnapshot())
                .isEqualTo("문제 해결 경험");

        var recommendations = recommendationRepository
                .findAllByInsight_Id(insight.getId());

        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0).getJobCompetencyIdSnapshot())
                .isEqualTo(competencyId);
        assertThat(recommendations.get(0).getMatchStatus())
                .isEqualTo(InsightJobRecommendationMatchStatus.MATCHED);
        assertThat(recommendations.get(0).getRecordIdSnapshot())
                .isEqualTo(recommendationRecordId);
        assertThat(recommendations.get(0).getReason())
                .isEqualTo("병목을 분석하고 개선한 과정이 구체적입니다.");
        assertThat(recommendations.get(0).getSimilarity())
                .isEqualTo(0.94);
    }

    @Test
    void savesNoMatchRecommendationWithoutRecordSnapshot() {
        Insight insight = createPendingInsight();
        UUID competencyId = UUID.randomUUID();

        InsightGenerationResult result = new InsightGenerationResult(
                insight.getId(),
                List.of(),
                List.of(),
                List.of(JobRecommendationResult.noMatch(
                        competencyId,
                        "데이터 분석"
                ))
        );

        resultWriter.complete(result);

        var recommendations = recommendationRepository
                .findAllByInsight_Id(insight.getId());

        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0).getMatchStatus())
                .isEqualTo(InsightJobRecommendationMatchStatus.NO_MATCH);
        assertThat(recommendations.get(0).getJobCompetencyIdSnapshot())
                .isEqualTo(competencyId);
        assertThat(recommendations.get(0).getRecordIdSnapshot()).isNull();
        assertThat(recommendations.get(0).getSimilarity()).isNull();
    }

    @Test
    void rollsBackAllResultsWhenPartialSaveFails() {
        // given
        Insight insight = createPendingInsight();
        UUID duplicatedStrengthTagId = UUID.randomUUID();

        // 같은 strengthTagId는 InsightStrength.strengthTagIdSnapshot으로
        // 매핑되어 uk_insight_strength_tag(insight_id,
        // strength_tag_id_snapshot) 유니크 제약을 위반한다.
        InsightGenerationResult invalidResult =
                new InsightGenerationResult(
                        insight.getId(),
                        List.of(
                                new StrengthResult(
                                        duplicatedStrengthTagId,
                                        "문제 해결",
                                        6,
                                        0.9,
                                        0.6,
                                        1,
                                        List.of(
                                                new StrengthRecordResult(
                                                        UUID.randomUUID(),
                                                        "첫 번째 기록",
                                                        SNAPSHOT_AT.minusDays(2)
                                                )
                                        )
                                ),
                                new StrengthResult(
                                        duplicatedStrengthTagId,
                                        "문제 해결",
                                        4,
                                        0.8,
                                        0.4,
                                        2,
                                        List.of(
                                                new StrengthRecordResult(
                                                        UUID.randomUUID(),
                                                        "두 번째 기록",
                                                        SNAPSHOT_AT.minusDays(1)
                                                )
                                        )
                                )
                        ),
                        List.of(
                                new TemplateResult(
                                        UUID.randomUUID(),
                                        "문제 해결 경험",
                                        10,
                                        1.0,
                                        1
                                )
                        ),
                        List.of(
                                new JobRecommendationResult(
                                        UUID.randomUUID(),
                                        "문제 해결",
                                        UUID.randomUUID(),
                                        "추천 기록",
                                        "문제 해결 경험",
                                        "추천 이유입니다.",
                                        0.9
                                )
                        )
                );

        // when & then
        assertThatThrownBy(
                () -> resultWriter.complete(invalidResult)
        ).isInstanceOf(PersistenceException.class);

        assertThat(strengthRepository
                .findAllByInsight_IdOrderByRankAsc(insight.getId()))
                .isEmpty();

        assertThat(templateRepository
                .findAllByInsight_IdOrderByRankAsc(insight.getId()))
                .isEmpty();

        assertThat(recommendationRepository
                .findAllByInsight_Id(insight.getId()))
                .isEmpty();

        Insight savedInsight = insightRepository.findById(insight.getId())
                .orElseThrow();

        assertThat(savedInsight.getStatus())
                .isEqualTo(InsightGenerationStatus.PENDING);
        assertThat(savedInsight.getCompletedAt()).isNull();
    }

    @Test
    void cannotCompleteAlreadyCompletedInsightAgain() {
        // given
        Insight insight = createPendingInsight();
        insight.complete(SNAPSHOT_AT.plusMinutes(1));
        insightRepository.saveAndFlush(insight);

        InsightGenerationResult result = emptyResult(insight.getId());

        // when & then
        assertThatThrownBy(
                () -> resultWriter.complete(result)
        ).isInstanceOf(IllegalStateException.class);

        Insight savedInsight = insightRepository.findById(insight.getId())
                .orElseThrow();

        assertThat(savedInsight.getStatus())
                .isEqualTo(InsightGenerationStatus.COMPLETED);
    }

    @Test
    void cannotCompleteFailedInsight() {
        // given
        Insight insight = createPendingInsight();
        insight.fail(
                SNAPSHOT_AT.plusMinutes(1),
                "AI_TIMEOUT",
                "AI 요청 시간이 초과되었습니다."
        );
        insightRepository.saveAndFlush(insight);

        InsightGenerationResult result = emptyResult(insight.getId());

        // when & then
        assertThatThrownBy(
                () -> resultWriter.complete(result)
        ).isInstanceOf(IllegalStateException.class);

        Insight savedInsight = insightRepository.findById(insight.getId())
                .orElseThrow();

        assertThat(savedInsight.getStatus())
                .isEqualTo(InsightGenerationStatus.FAILED);
        assertThat(savedInsight.getFailureCode())
                .isEqualTo("AI_TIMEOUT");
    }

    private InsightGenerationResult emptyResult(UUID insightId) {
        return new InsightGenerationResult(
                insightId,
                List.of(),
                List.of(),
                List.of()
        );
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
