package com.itcotato.dortfolio.global.ai.observability.dto;

import java.util.UUID;

public record AiErrorResponse(
        UUID requestId,
        String provider,
        String modelId,
        String status,
        String errorCode,
        long latencyMs
) {
}
