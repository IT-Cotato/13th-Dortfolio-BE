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
    DUPLICATE_TEMPLATE_SELECTION(HttpStatus.BAD_REQUEST, "T005", "같은 템플릿을 중복 선택할 수 없습니다."),

    // Record 에러
    RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "기록을 찾을 수 없습니다."),
    RECORD_FORBIDDEN(HttpStatus.FORBIDDEN, "R002", "해당 기록에 접근할 수 없습니다."),
    RECORD_TEMPLATE_NOT_CONNECTED(HttpStatus.BAD_REQUEST, "R003", "활동에 연결된 템플릿만 기록에 사용할 수 있습니다."),
    RECORD_MEMO_NOT_FOUND(HttpStatus.NOT_FOUND, "R004", "메모를 찾을 수 없습니다."),
    RECORD_MEMO_ACTIVITY_MISMATCH(HttpStatus.BAD_REQUEST, "R005", "기록과 같은 활동의 메모만 연결할 수 있습니다."),
    RECORD_QUESTION_NOT_FOUND(HttpStatus.BAD_REQUEST, "R006", "템플릿에 포함된 질문에만 답변할 수 있습니다."),
    RECORD_REQUIRED_ANSWER_MISSING(HttpStatus.BAD_REQUEST, "R007", "필수 항목에 입력해주세요."),
    DUPLICATE_RECORD_MEMO_SELECTION(HttpStatus.BAD_REQUEST, "R008", "같은 메모를 중복 선택할 수 없습니다."),
    DUPLICATE_RECORD_ANSWER_SELECTION(HttpStatus.BAD_REQUEST, "R009", "같은 질문에 대한 답변을 중복 입력할 수 없습니다."),
    RECORD_STATUS_TRANSITION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "R010", "완료된 기록은 기록 중 상태로 변경할 수 없습니다."),
    RECORD_RESTORE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "R011", "기록을 복구할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
