package com.itcotato.dortfolio.domain.insight.generation.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/* Insight 생성 작업이 계산한 최종 결과 */
public record InsightGenerationResult(
        UUID insightId,
        List<StrengthResult> strengths,
        List<TemplateResult> templates,
        List<JobRecommendationResult> recommendations
) {

    /* 외부에서 전달한 가변 리스트가 나중에 변경되지 않도록 복사 */
    public InsightGenerationResult {
        strengths = List.copyOf(strengths);
        templates = List.copyOf(templates);
        recommendations = List.copyOf(recommendations);
    }

    /* 강점 TOP 5 항목 */
    public record StrengthResult(
            UUID strengthTagId,
            String strengthName,
            int recordCount,
            double averageScore,
            double ratio,
            int rank,
            List<StrengthRecordResult> records
    ) {

        public StrengthResult {
            records = List.copyOf(records);
        }
    }

    /* 특정 강점과 연결되는 기록의 스냅샷 */
    public record StrengthRecordResult(
            UUID recordId,
            String recordTitle,
            LocalDateTime recordCompletedAt
    ) {
    }

    /* 템플릿 TOP 4 항목 */
    public record TemplateResult(
            UUID templateId,
            String templateName,
            int recordCount,
            double ratio,
            int rank
    ) {
    }

    /* 직무 역량별 최종 추천 결과 */
    public record JobRecommendationResult(
            boolean matched,
            UUID jobCompetencyId,
            String competencyName,
            UUID recordId,
            String recordTitle,
            String templateName,
            String reason,
            Double similarity
    ) {
        public JobRecommendationResult {
            if (matched && (recordId == null
                    || recordTitle == null
                    || templateName == null
                    || reason == null
                    || similarity == null)) {
                throw new IllegalArgumentException("Matched recommendation requires record details");
            }
            if (!matched && (recordId != null
                    || recordTitle != null
                    || templateName != null
                    || reason != null
                    || similarity != null)) {
                throw new IllegalArgumentException("Unmatched recommendation cannot contain record details");
            }
        }

        public JobRecommendationResult(
                UUID jobCompetencyId,
                String competencyName,
                UUID recordId,
                String recordTitle,
                String templateName,
                String reason,
                double similarity
        ) {
            this(
                    true,
                    jobCompetencyId,
                    competencyName,
                    recordId,
                    recordTitle,
                    templateName,
                    reason,
                    similarity
            );
        }

        public static JobRecommendationResult noMatch(
                UUID jobCompetencyId,
                String competencyName
        ) {
            return new JobRecommendationResult(
                    false,
                    jobCompetencyId,
                    competencyName,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }
    }
}
