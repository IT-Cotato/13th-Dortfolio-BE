package com.itcotato.dortfolio.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Global 공통 에러
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "G001", "올바르지 않은 입력값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "G002", "지원하지 않는 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G003", "서버 내부 오류가 발생했습니다."),

    // User / Auth 에러
    USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "G001", "올바르지 않은 입력값입니다."),
    SOCIAL_USER_PASSWORD_RESET_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "U002", "소셜 로그인 계정은 비밀번호를 재설정할 수 없습니다."),
    INVALID_RESET_TOKEN(HttpStatus.BAD_REQUEST, "U003", "만료되었거나 유효하지 않은 비밀번호 재설정 토큰입니다."),
    RESET_EMAIL_MISMATCH(HttpStatus.BAD_REQUEST, "U004", "잘못된 접근입니다. 이메일 정보가 일치하지 않습니다."),

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
