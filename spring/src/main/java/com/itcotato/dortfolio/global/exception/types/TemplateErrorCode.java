package com.itcotato.dortfolio.global.exception.types;

import com.itcotato.dortfolio.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TemplateErrorCode implements ErrorCode {

    TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "템플릿을 찾을 수 없습니다."),
    TEMPLATE_FORBIDDEN(HttpStatus.FORBIDDEN, "T002", "해당 템플릿에 접근할 수 없습니다."),
    BUILTIN_TEMPLATE_DELETION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "T003", "기본 제공 템플릿은 삭제할 수 없습니다."),
    TEMPLATE_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "T006", "템플릿을 요청한 사용자를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
