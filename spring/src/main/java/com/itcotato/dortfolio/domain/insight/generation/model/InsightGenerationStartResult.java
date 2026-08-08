package com.itcotato.dortfolio.domain.insight.generation.model;

import com.itcotato.dortfolio.domain.insight.entity.InsightGenerationStatus;
import java.util.UUID;

/* Insight 생성 요청 처리 결과 */
public record InsightGenerationStartResult(
        UUID generationId,
        InsightGenerationStatus status
) {

    public static InsightGenerationStartResult pending(
            UUID generationId
    ) {
        return new InsightGenerationStartResult(
                generationId,
                InsightGenerationStatus.PENDING
        );
    }
}