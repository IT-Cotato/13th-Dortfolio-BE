package com.itcotato.dortfolio.domain.insight.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Insight 생성 가능 여부 응답")
public record InsightEligibilityApiResponse(

        @Schema(
                description = "현재 Insight 생성 가능 여부",
                example = "true"
        )
        boolean eligible,

        @Schema(
                description = "생성 가능 여부 판정 사유",
                example = "AVAILABLE"
        )
        InsightEligibilityReason reason,

        @Schema(
                description = "다음 생성 가능 시각. 대기 시간이 없으면 null",
                example = "2026-08-07T16:30:00",
                nullable = true
        )
        LocalDateTime nextAvailableAt,

        @Schema(
                description = "현재 Insight 생성에 사용할 수 있는 분석 완료 기록 수",
                example = "12"
        )
        long currentRecordCount,

        @Schema(
                description = "Insight 생성에 필요한 최소 기록 수",
                example = "10"
        )
        int requiredRecordCount,

        @Schema(
                description = "Insight 분석 대상인 작성 완료 기록 수",
                example = "13"
        )
        long totalRecordCount,

        @Schema(
                description = "AI 분석 완료 기록 수",
                example = "5"
        )
        long analysisCompletedCount,

        @Schema(
                description = "AI 분석 실패 기록 수",
                example = "7"
        )
        long analysisFailedCount,

        @Schema(
                description = "AI 분석 대기 또는 진행 중인 기록 수",
                example = "1"
        )
        long analysisInProgressCount
) {

    public static InsightEligibilityApiResponse from(
            InsightEligibilityResponse response
    ) {
        return new InsightEligibilityApiResponse(
                response.eligible(),
                response.reason(),
                response.nextAvailableAt(),
                response.analyzedRecordCount(),
                response.requiredRecordCount(),
                response.totalRecordCount(),
                response.analysisCompletedCount(),
                response.analysisFailedCount(),
                response.analysisInProgressCount()
        );
    }
}
