package com.itcotato.dortfolio.domain.insight.dto.res;

import com.itcotato.dortfolio.domain.insight.entity.Insight;
import com.itcotato.dortfolio.domain.insight.entity.InsightGenerationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Insight 생성 상태 응답")
public record InsightGenerationStatusResponse(

        @Schema(
                description = "Insight generation ID",
                example = "c2869cc7-9d98-43e4-87ae-7c2f284f3138"
        )
        UUID generationId,

        @Schema(
                description = "생성 상태",
                example = "PENDING"
        )
        InsightGenerationStatus status,

        @Schema(
                description = "생성 요청 시각",
                example = "2026-08-06T16:30:00"
        )
        LocalDateTime requestedAt,

        @Schema(
                description = "생성 완료 시각",
                example = "2026-08-06T16:30:08",
                nullable = true
        )
        LocalDateTime completedAt,

        @Schema(
                description = "생성 실패 시각",
                example = "2026-08-06T16:30:08",
                nullable = true
        )
        LocalDateTime failedAt,

        @Schema(
                description = "실패 코드",
                example = "I002",
                nullable = true
        )
        String failureCode,

        @Schema(
                description = "실패 메시지",
                example = "Insight 추천 서비스를 사용할 수 없습니다.",
                nullable = true
        )
        String failureMessage
) {

    public static InsightGenerationStatusResponse from(
            Insight insight
    ) {
        return new InsightGenerationStatusResponse(
                insight.getId(),
                insight.getStatus(),
                insight.getRequestedAt(),
                insight.getCompletedAt(),
                insight.getFailedAt(),
                insight.getFailureCode(),
                insight.getFailureMessage()
        );
    }
}