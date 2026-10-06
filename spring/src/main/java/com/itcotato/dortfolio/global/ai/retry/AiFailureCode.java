package com.itcotato.dortfolio.global.ai.retry;

import com.itcotato.dortfolio.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AiFailureCode implements ErrorCode {
    TEMPORARY(HttpStatus.SERVICE_UNAVAILABLE, "AI_TEMPORARY_ERROR", "AI 서비스에 일시적인 오류가 발생했습니다."),
    INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "AI_INVALID_RESPONSE", "AI 응답이 올바르지 않습니다."),
    CONFIGURATION(HttpStatus.INTERNAL_SERVER_ERROR, "AI_CONFIGURATION_ERROR", "AI 서비스 설정을 확인해야 합니다."),
    OUTPUT_LIMIT(HttpStatus.BAD_GATEWAY, "AI_OUTPUT_LIMIT", "AI 생성 상한에 도달했습니다."),
    REJECTED(HttpStatus.BAD_GATEWAY, "AI_REQUEST_REJECTED", "AI 요청을 처리하지 못했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
