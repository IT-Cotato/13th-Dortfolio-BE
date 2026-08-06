package com.itcotato.dortfolio.domain.insight.dto.res;

import com.itcotato.dortfolio.domain.insight.entity.InsightGenerationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Insight 생성 요청 응답")
public record InsightGenerationCreateResponse(

        @Schema(
                description = "Insight generation ID",
                example = "c2869cc7-9d98-43e4-87ae-7c2f284f3138"
        )
        UUID generationId,

        @Schema(
                description = "현재 생성 상태",
                example = "PENDING"
        )
        InsightGenerationStatus status
) {

    public static InsightGenerationCreateResponse pending(
            UUID generationId
    ) {
        return new InsightGenerationCreateResponse(
                generationId,
                InsightGenerationStatus.PENDING
        );
    }
}