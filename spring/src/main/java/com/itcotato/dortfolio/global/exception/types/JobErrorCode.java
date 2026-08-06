package com.itcotato.dortfolio.global.exception.types;

import com.itcotato.dortfolio.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum JobErrorCode implements ErrorCode {

    JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "J001", "존재하지 않는 직무입니다."),
    JOB_COMPETENCY_NOT_FOUND(HttpStatus.NOT_FOUND, "J002", "존재하지 않는 직무 역량입니다."),
    JOB_COMPETENCY_EMBEDDING_AI_SERVICE_FAILED(
            HttpStatus.SERVICE_UNAVAILABLE, "J003", "직무 역량 임베딩 서비스를 사용할 수 없습니다."
    ),
    JOB_COMPETENCY_EMBEDDING_INVALID_RESPONSE(
            HttpStatus.BAD_GATEWAY, "J004", "직무 역량 임베딩 응답이 올바르지 않습니다."
    ),
    JOB_COMPETENCY_EMBEDDING_NOT_READY(
            HttpStatus.SERVICE_UNAVAILABLE, "J005", "직무 역량 임베딩이 준비되지 않았습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
