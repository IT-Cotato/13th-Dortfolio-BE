package com.itcotato.dortfolio.domain.insight.support.fixture;

import com.itcotato.dortfolio.domain.insight.dto.res.InsightEligibilityReason;
import com.itcotato.dortfolio.domain.insight.dto.res.InsightEligibilityResponse;
import com.itcotato.dortfolio.domain.insight.query.AnalyzedRecordSnapshot;
import com.itcotato.dortfolio.domain.insight.recommendation.RecommendationCandidate;
import com.itcotato.dortfolio.domain.insight.recommendation.RecommendationRequest;
import com.itcotato.dortfolio.domain.insight.recommendation.RecommendationResult;
import com.itcotato.dortfolio.domain.insight.statistics.StrengthStatistic;
import com.itcotato.dortfolio.domain.insight.statistics.TemplateStatistic;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class InsightTestFixture {
    public static final UUID USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    public static final UUID RECORD_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000002");

    public static final UUID TEMPLATE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000003");

    public static final UUID STRENGTH_TAG_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000004");

    public static final UUID JOB_COMPETENCY_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000005");

    public static final LocalDateTime SNAPSHOT_AT =
            LocalDateTime.of(2026, 8, 1, 15, 0);

    private InsightTestFixture() {
    }

    public static InsightEligibilityResponse availableEligibility() {
        return new InsightEligibilityResponse(
                true,
                InsightEligibilityReason.AVAILABLE,
                null,
                10,
                10,
                10
        );
    }

    public static InsightEligibilityResponse cooldownEligibility() {
        return new InsightEligibilityResponse(
                false,
                InsightEligibilityReason.COOLDOWN,
                SNAPSHOT_AT.plusHours(24),
                12,
                12,
                10
        );
    }

    public static AnalyzedRecordSnapshot analyzedRecord() {
        return new AnalyzedRecordSnapshot(
                RECORD_ID,
                "추천 알고리즘 개선",
                LocalDateTime.of(2026, 7, 30, 10, 0),
                TEMPLATE_ID,
                "문제 해결 경험",
                "추천 기준을 분석하고 개선한 경험입니다.",
                List.of("추천 기준을 다시 정의했습니다."),
                List.of(new AnalyzedRecordSnapshot.StrengthTagSnapshot(
                        STRENGTH_TAG_ID,
                        "문제 해결",
                        0.9f
                ))
        );
    }

    public static StrengthStatistic strengthStatistic() {
        return new StrengthStatistic(
                STRENGTH_TAG_ID,
                "문제 해결",
                8,
                0.87,
                0.8,
                1
        );
    }

    public static TemplateStatistic templateStatistic() {
        return new TemplateStatistic(
                TEMPLATE_ID,
                "문제 해결 경험",
                6,
                0.6,
                1
        );
    }

    public static RecommendationCandidate recommendationCandidate() {
        return new RecommendationCandidate(
                RECORD_ID,
                "추천 알고리즘 개선",
                "문제 해결 경험",
                "추천 기준을 분석하고 개선한 경험입니다.",
                List.of("추천 기준을 다시 정의했습니다."),
                0.91
        );
    }

    public static RecommendationRequest recommendationRequest() {
        return new RecommendationRequest(
                JOB_COMPETENCY_ID,
                "백엔드 개발자",
                "문제 해결",
                "문제의 원인을 분석하고 해결 방법을 적용하는 역량",
                List.of(recommendationCandidate())
        );
    }

    public static RecommendationResult recommendationResult() {
        return new RecommendationResult(
                JOB_COMPETENCY_ID,
                RECORD_ID,
                "문제의 원인을 분석하고 개선한 과정이 구체적으로 드러납니다."
        );
    }
}
