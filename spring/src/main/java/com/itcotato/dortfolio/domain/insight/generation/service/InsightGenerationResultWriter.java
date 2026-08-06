package com.itcotato.dortfolio.domain.insight.generation.service;

import com.itcotato.dortfolio.domain.insight.entity.Insight;
import com.itcotato.dortfolio.domain.insight.entity.InsightJobRecommendation;
import com.itcotato.dortfolio.domain.insight.entity.InsightStrength;
import com.itcotato.dortfolio.domain.insight.entity.InsightStrengthRecord;
import com.itcotato.dortfolio.domain.insight.entity.InsightTemplateStatistic;
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
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.InsightErrorCode;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/* Insight 생성 결과 전체를 하나의 트랜잭션으로 저장 */
@Component
@RequiredArgsConstructor
public class InsightGenerationResultWriter {

    private final InsightRepository insightRepository;
    private final InsightStrengthRepository strengthRepository;
    private final InsightStrengthRecordRepository strengthRecordRepository;
    private final InsightTemplateStatisticRepository templateRepository;
    private final InsightJobRecommendationRepository recommendationRepository;

    private final EntityManager entityManager;
    private final Clock clock;

    /* 생성 결과를 저장하고 Insight를 완료 처리 */
    @Transactional
    public void complete(InsightGenerationResult result) {
        Insight insight = insightRepository.findById(result.insightId())
                .orElseThrow(() -> new CustomException(
                        InsightErrorCode.INSIGHT_NOT_FOUND
                ));

        saveStrengths(insight, result);
        saveTemplates(insight, result);
        saveRecommendations(insight, result);

        entityManager.flush();

        LocalDateTime completedAt = LocalDateTime.now(clock);
        insight.complete(completedAt);

        entityManager.flush();
    }

    /* 강점 저장 */
    private void saveStrengths(
            Insight insight,
            InsightGenerationResult result
    ) {
        for (StrengthResult strengthResult : result.strengths()) {
            InsightStrength strength = InsightStrength.create(
                    insight,
                    strengthResult.strengthTagId(),
                    strengthResult.strengthName(),
                    strengthResult.recordCount(),
                    strengthResult.averageScore(),
                    strengthResult.ratio(),
                    strengthResult.rank()
            );

            strengthRepository.save(strength);

            saveStrengthRecords(
                    strength,
                    strengthResult
            );
        }
    }

    /* 강점 연결 기록 저장 */
    private void saveStrengthRecords(
            InsightStrength strength,
            StrengthResult strengthResult
    ) {
        for (StrengthRecordResult recordResult
                : strengthResult.records()) {

            InsightStrengthRecord strengthRecord =
                    InsightStrengthRecord.create(
                            strength,
                            recordResult.recordId(),
                            recordResult.recordTitle(),
                            recordResult.recordCompletedAt()
                    );

            strengthRecordRepository.save(strengthRecord);
        }
    }

    /* 템플릿 통계 저장 */
    private void saveTemplates(
            Insight insight,
            InsightGenerationResult result
    ) {
        for (TemplateResult templateResult : result.templates()) {
            InsightTemplateStatistic template =
                    InsightTemplateStatistic.create(
                            insight,
                            templateResult.templateId(),
                            templateResult.templateName(),
                            templateResult.recordCount(),
                            templateResult.ratio(),
                            templateResult.rank()
                    );

            templateRepository.save(template);
        }
    }

    /* 직무 역량별 추천 저장 */
    private void saveRecommendations(
            Insight insight,
            InsightGenerationResult result
    ) {
        for (JobRecommendationResult recommendationResult
                : result.recommendations()) {

            InsightJobRecommendation recommendation =
                    InsightJobRecommendation.create(
                            insight,
                            recommendationResult.jobCompetencyId(),
                            recommendationResult.competencyName(),
                            recommendationResult.recordId(),
                            recommendationResult.recordTitle(),
                            recommendationResult.templateName(),
                            recommendationResult.reason(),
                            recommendationResult.similarity()
                    );

            recommendationRepository.save(recommendation);
        }
    }
}