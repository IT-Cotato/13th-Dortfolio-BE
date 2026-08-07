package com.itcotato.dortfolio.domain.insight.dto.res;

import com.itcotato.dortfolio.domain.insight.entity.InsightGenerationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "최신 완료 Insight 조회 응답")
public record LatestInsightResponse(

        @Schema(description = "Insight ID")
        UUID insightId,

        @Schema(description = "생성 당시 직무 스냅샷")
        JobSnapshotResponse job,

        @Schema(
                description = "Insight 생성에 사용한 기록 기준 시각",
                example = "2026-08-06T16:30:00"
        )
        LocalDateTime recordSnapshotAt,

        @Schema(
                description = "Insight 생성 완료 시각",
                example = "2026-08-06T16:30:12"
        )
        LocalDateTime completedAt,

        @Schema(
                description = "Insight 생성에 사용한 분석 완료 기록 수",
                example = "12"
        )
        int analyzedRecordCount,

        @Schema(description = "강점 TOP 5")
        List<StrengthResponse> strengths,

        @Schema(description = "템플릿 TOP 4")
        List<TemplateStatisticResponse> templates,

        @Schema(description = "직무 역량별 추천 기록")
        List<JobRecommendationResponse> recommendations,

        @Schema(description = "마지막 완료 Insight 이후 변경 내역")
        ChangeSummaryResponse changes,

        @Schema(
                description = "현재 진행 중인 Insight generation. 없으면 null",
                nullable = true
        )
        CurrentGenerationResponse currentGeneration
) {

    public LatestInsightResponse {
        strengths = List.copyOf(strengths);
        templates = List.copyOf(templates);
        recommendations = List.copyOf(recommendations);
    }

    @Schema(description = "마지막 완료 Insight 이후 현재까지의 변경 내역")
    public record ChangeSummaryResponse(

            @Schema(
                    description = "마지막 완료 Insight의 기준 시각 이후 새로 분석 완료되어 Insight에 사용할 수 있는 기록 수",
                    example = "3"
            )
            long newCompletedRecordCount,

            @Schema(
                    description = "현재 희망 직무가 Insight 생성 당시 직무와 다른지 여부",
                    example = "false"
            )
            boolean desiredJobChanged,

            @Schema(
                    description = "현재 분석 진행 중인 유효 기록 수",
                    example = "1"
            )
            long analysisPendingRecordCount,

            @Schema(
                    description = "현재 분석에 실패한 유효 기록 수",
                    example = "0"
            )
            long analysisFailedRecordCount
    ) {
    }

    @Schema(description = "Insight 생성 당시 직무 스냅샷")
    public record JobSnapshotResponse(

            @Schema(description = "직무 ID")
            UUID jobId,

            @Schema(
                    description = "직무 이름",
                    example = "백엔드 개발자"
            )
            String jobName
    ) {
    }

    @Schema(description = "강점 통계")
    public record StrengthResponse(

            @Schema(description = "강점 태그 ID")
            UUID strengthTagId,

            @Schema(
                    description = "강점 이름",
                    example = "문제 해결"
            )
            String strengthName,

            @Schema(
                    description = "강점이 나타난 기록 수",
                    example = "7"
            )
            int recordCount,

            @Schema(
                    description = "강점 평균 점수",
                    example = "0.91"
            )
            double averageScore,

            @Schema(
                    description = "전체 분석 기록 대비 비율",
                    example = "0.5833"
            )
            double ratio,

            @Schema(
                    description = "강점 순위",
                    example = "1"
            )
            int rank,

            @Schema(description = "강점과 연결된 기록")
            List<StrengthRecordResponse> records
    ) {

        public StrengthResponse {
            records = List.copyOf(records);
        }
    }

    @Schema(description = "강점 연결 기록 스냅샷")
    public record StrengthRecordResponse(

            @Schema(description = "기록 ID")
            UUID recordId,

            @Schema(
                    description = "Insight 생성 당시 기록 제목",
                    example = "API 응답 속도 개선"
            )
            String recordTitle,

            @Schema(
                    description = "Insight 생성 당시 기록 완료 시각",
                    example = "2026-08-05T13:00:00"
            )
            LocalDateTime completedAt,

            @Schema(
                    description = "현재 기록 상세 화면으로 이동 가능한지 여부",
                    example = "true"
            )
            boolean navigationAvailable
    ) {
    }

    @Schema(description = "템플릿 분포 통계")
    public record TemplateStatisticResponse(

            @Schema(description = "템플릿 ID")
            UUID templateId,

            @Schema(
                    description = "Insight 생성 당시 템플릿 이름",
                    example = "문제 해결 경험"
            )
            String templateName,

            @Schema(
                    description = "해당 템플릿 기록 수",
                    example = "6"
            )
            int recordCount,

            @Schema(
                    description = "전체 분석 기록 대비 비율",
                    example = "0.5"
            )
            double ratio,

            @Schema(
                    description = "템플릿 순위",
                    example = "1"
            )
            int rank
    ) {
    }

    @Schema(description = "직무 역량별 추천 기록")
    public record JobRecommendationResponse(

            @Schema(description = "직무 역량 ID")
            UUID jobCompetencyId,

            @Schema(
                    description = "직무 역량 이름",
                    example = "문제 해결"
            )
            String competencyName,

            @Schema(description = "추천 기록 ID")
            UUID recordId,

            @Schema(
                    description = "Insight 생성 당시 기록 제목",
                    example = "API 응답 속도 개선"
            )
            String recordTitle,

            @Schema(
                    description = "Insight 생성 당시 템플릿 이름",
                    example = "문제 해결 경험"
            )
            String templateName,

            @Schema(
                    description = "AI 추천 이유",
                    example = "문제를 분석하고 해결한 과정이 구체적입니다."
            )
            String reason,

            @Schema(
                    description = "역량과 기록의 코사인 유사도",
                    example = "0.94"
            )
            double similarity,

            @Schema(
                    description = "현재 기록 상세 화면으로 이동 가능한지 여부",
                    example = "true"
            )
            boolean navigationAvailable
    ) {
    }

    @Schema(description = "현재 진행 중인 generation")
    public record CurrentGenerationResponse(

            @Schema(description = "진행 중인 generation ID")
            UUID generationId,

            @Schema(
                    description = "현재 생성 상태",
                    example = "PENDING"
            )
            InsightGenerationStatus status,

            @Schema(
                    description = "생성 요청 시각",
                    example = "2026-08-06T17:00:00"
            )
            LocalDateTime requestedAt
    ) {
    }
}
