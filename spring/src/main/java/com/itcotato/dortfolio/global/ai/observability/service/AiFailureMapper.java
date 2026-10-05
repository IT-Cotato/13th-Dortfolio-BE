package com.itcotato.dortfolio.global.ai.observability.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itcotato.dortfolio.global.ai.observability.dto.AiErrorResponse;
import com.itcotato.dortfolio.global.ai.observability.entity.AiCallStatus;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class AiFailureMapper {

    private final ObjectMapper objectMapper;

    public Failure fromHttpError(
            UUID requestId,
            RestClientResponseException exception,
            String expectedProvider,
            String expectedModelId,
            long measuredLatencyMs
    ) {
        AiErrorResponse body = readErrorBody(exception);

        if (body != null
                && requestId.equals(body.requestId())
                && StringUtils.hasText(body.provider())
                && StringUtils.hasText(body.modelId())) {
            return new Failure(
                    body.provider(),
                    body.modelId(),
                    resolveStatus(
                            body.status(),
                            exception.getStatusCode().value()
                    ),
                    safeErrorCode(
                            body.errorCode(),
                            exception.getStatusCode().value()
                    ),
                    nonNegative(body.latencyMs())
            );
        }

        int httpStatus = exception.getStatusCode().value();

        return new Failure(
                expectedProvider,
                expectedModelId,
                statusFromHttp(httpStatus),
                "HTTP_" + httpStatus,
                nonNegative(measuredLatencyMs)
        );
    }

    public Failure fromTransportError(
            RestClientException exception,
            String expectedProvider,
            String expectedModelId,
            long measuredLatencyMs
    ) {
        boolean timeout = isTimeout(exception);

        return new Failure(
                expectedProvider,
                expectedModelId,
                timeout
                        ? AiCallStatus.TIMEOUT
                        : AiCallStatus.UNKNOWN_ERROR,
                timeout
                        ? "REQUEST_TIMEOUT"
                        : "TRANSPORT_ERROR",
                nonNegative(measuredLatencyMs)
        );
    }

    private AiErrorResponse readErrorBody(
            RestClientResponseException exception
    ) {
        try {
            return objectMapper.readValue(
                    exception.getResponseBodyAsByteArray(),
                    AiErrorResponse.class
            );
        } catch (IOException | IllegalArgumentException ignored) {
            return null;
        }
    }

    private AiCallStatus resolveStatus(
            String responseStatus,
            int httpStatus
    ) {
        if (StringUtils.hasText(responseStatus)) {
            try {
                AiCallStatus parsed = AiCallStatus.valueOf(
                        responseStatus
                );

                if (parsed != AiCallStatus.SUCCESS) {
                    return parsed;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        return statusFromHttp(httpStatus);
    }

    private AiCallStatus statusFromHttp(int httpStatus) {
        if (httpStatus == 429) {
            return AiCallStatus.RATE_LIMITED;
        }
        if (httpStatus == 408 || httpStatus == 504) {
            return AiCallStatus.TIMEOUT;
        }
        if (httpStatus >= 500) {
            return AiCallStatus.PROVIDER_ERROR;
        }
        return AiCallStatus.UNKNOWN_ERROR;
    }

    private String safeErrorCode(
            String responseCode,
            int httpStatus
    ) {
        if (responseCode != null
                && responseCode.matches("[A-Z0-9_]{1,100}")) {
            return responseCode;
        }

        return "HTTP_" + httpStatus;
    }

    private boolean isTimeout(Throwable exception) {
        Throwable current = exception;

        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }

            current = current.getCause();
        }

        return exception instanceof ResourceAccessException
                && exception.getMessage() != null
                && exception.getMessage()
                .toLowerCase()
                .contains("timed out");
    }

    private long nonNegative(long value) {
        return Math.max(0L, value);
    }

    public record Failure(
            String provider,
            String modelId,
            AiCallStatus status,
            String errorCode,
            long latencyMs
    ) {
    }
}
