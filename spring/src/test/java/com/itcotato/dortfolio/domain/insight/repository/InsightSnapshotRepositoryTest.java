package com.itcotato.dortfolio.domain.insight.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.itcotato.dortfolio.domain.insight.entity.Insight;
import com.itcotato.dortfolio.domain.insight.entity.InsightJobRecommendation;
import com.itcotato.dortfolio.domain.insight.entity.InsightStrength;
import com.itcotato.dortfolio.domain.insight.entity.InsightStrengthRecord;
import com.itcotato.dortfolio.domain.insight.entity.InsightTemplateStatistic;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class InsightSnapshotRepositoryTest {

    private static final LocalDateTime SNAPSHOT_AT = LocalDateTime.of(2026, 8, 1, 10, 0);

    @Autowired
    private InsightRepository insightRepository;

    @Autowired
    private InsightStrengthRepository strengthRepository;

    @Autowired
    private InsightStrengthRecordRepository strengthRecordRepository;

    @Autowired
    private InsightTemplateStatisticRepository templateStatisticRepository;

    @Autowired
    private InsightJobRecommendationRepository recommendationRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void savesAndReadsResultSnapshots() {
        Insight insight = createInsight();
        InsightStrength secondStrength = strengthRepository.save(InsightStrength.create(
                insight, UUID.randomUUID(), "협업", 4, 0.7, 0.4, 2
        ));
        InsightStrength firstStrength = strengthRepository.save(InsightStrength.create(
                insight, UUID.randomUUID(), "문제 해결", 6, 0.9, 0.6, 1
        ));
        InsightStrengthRecord strengthRecord = strengthRecordRepository.save(
                InsightStrengthRecord.create(
                        firstStrength,
                        UUID.randomUUID(),
                        "성능 병목 개선",
                        SNAPSHOT_AT.minusDays(1)
                )
        );
        InsightTemplateStatistic secondTemplate = templateStatisticRepository.save(
                InsightTemplateStatistic.create(
                        insight, UUID.randomUUID(), "협업 경험", 4, 0.4, 2
                )
        );
        InsightTemplateStatistic firstTemplate = templateStatisticRepository.save(
                InsightTemplateStatistic.create(
                        insight, UUID.randomUUID(), "문제 해결 경험", 6, 0.6, 1
                )
        );
        InsightJobRecommendation recommendation = recommendationRepository.save(
                InsightJobRecommendation.create(
                        insight,
                        UUID.randomUUID(),
                        "문제 해결",
                        UUID.randomUUID(),
                        "성능 병목 개선",
                        "문제 해결 경험",
                        "원인 분석과 개선 결과가 구체적입니다.",
                        0.92
                )
        );

        assertThat(strengthRepository.findAllByInsight_IdOrderByRankAsc(insight.getId()))
                .containsExactly(firstStrength, secondStrength);
        assertThat(strengthRecordRepository.findAllByInsightStrength_Id(firstStrength.getId()))
                .containsExactly(strengthRecord);
        assertThat(templateStatisticRepository.findAllByInsight_IdOrderByRankAsc(insight.getId()))
                .containsExactly(firstTemplate, secondTemplate);
        assertThat(recommendationRepository.findAllByInsight_Id(insight.getId()))
                .containsExactly(recommendation);
        assertThat(recommendation.getRecordTitleSnapshot()).isEqualTo("성능 병목 개선");
    }

    @Test
    void preventsDuplicateStrengthForSameInsight() {
        Insight insight = createInsight();
        UUID strengthTagId = UUID.randomUUID();
        strengthRepository.saveAndFlush(InsightStrength.create(
                insight, strengthTagId, "문제 해결", 6, 0.6, 0.6, 1
        ));

        assertThatThrownBy(() -> strengthRepository.saveAndFlush(InsightStrength.create(
                insight, strengthTagId, "문제 해결", 6, 0.6, 0.6, 2
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Insight createInsight() {
        User user = userRepository.save(User.of(
                UUID.randomUUID() + "@test.com",
                "encoded-password",
                "인사이트 테스트"
        ));
        Insight insight = Insight.pending(
                user,
                UUID.randomUUID(),
                "백엔드 개발자",
                SNAPSHOT_AT,
                10,
                SNAPSHOT_AT
        );
        return insightRepository.save(insight);
    }
}
