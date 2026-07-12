package com.itcotato.dortfolio.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Global 공통 에러
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "G001", "올바르지 않은 입력값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "G002", "지원하지 않는 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G003", "서버 내부 오류가 발생했습니다."),

    // Template 에러
    TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "템플릿을 찾을 수 없습니다."),
    TEMPLATE_FORBIDDEN(HttpStatus.FORBIDDEN, "T002", "해당 템플릿에 접근할 수 없습니다."),
    BUILTIN_TEMPLATE_MODIFICATION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "T003", "기본 제공 템플릿은 수정하거나 삭제할 수 없습니다."),
    ACTIVITY_TEMPLATE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "T004", "활동에 연결할 템플릿은 최대 4개까지 선택할 수 있습니다."),
    DUPLICATE_TEMPLATE_SELECTION(HttpStatus.BAD_REQUEST, "T005", "같은 템플릿을 중복 선택할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
