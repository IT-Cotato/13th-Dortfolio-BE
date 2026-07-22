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
    BUILTIN_TEMPLATE_MODIFICATION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "T003", "기본 제공 템플릿은 수정하거나 삭제할 수 없습니다."),
    ACTIVITY_TEMPLATE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "T004", "활동에 연결할 템플릿은 최대 4개까지 선택할 수 있습니다."),
    DUPLICATE_TEMPLATE_SELECTION(HttpStatus.BAD_REQUEST, "T005", "같은 템플릿을 중복 선택할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}