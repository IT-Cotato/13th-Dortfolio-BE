package com.itcotato.dortfolio.global.ai.observability.dto;

import java.util.UUID;

public record AiUsageResponse(
        UUID requestId,
        String provider,
        String modelId,
        Long inputTokens,
        Long outputTokens,
        long latencyMs
) {
}
