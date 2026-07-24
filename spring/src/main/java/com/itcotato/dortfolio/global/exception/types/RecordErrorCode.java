package com.itcotato.dortfolio.global.exception.types;

import com.itcotato.dortfolio.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RecordErrorCode implements ErrorCode {

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
    RECORD_RESTORE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "R011", "기록을 복구할 수 없습니다."),
    RECORD_PERMANENT_DELETE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "R012", "삭제된 기록만 영구 삭제할 수 있습니다."),
    RECORD_INVALID_PAGE_REQUEST(HttpStatus.BAD_REQUEST, "R013", "올바르지 않은 기록 목록 페이지 요청입니다."),
    RECORD_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "R014", "기록을 요청한 사용자를 찾을 수 없습니다."),
    RECORD_ACTIVITY_NOT_FOUND(HttpStatus.NOT_FOUND, "R015", "기록에 사용할 활동을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
