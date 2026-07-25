package com.itcotato.dortfolio.global.exception.types;

import com.itcotato.dortfolio.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ActivityErrorCode implements ErrorCode {

    ACTIVITY_NOT_FOUND(HttpStatus.NOT_FOUND, "A001", "존재하지 않는 활동입니다."),
    ACTIVITY_TYPE_NOT_FOUND(HttpStatus.NOT_FOUND, "A002", "존재하지 않는 활동 종류입니다."),
    DELETED_ACTIVITY_NOT_LINKABLE(HttpStatus.BAD_REQUEST, "A003", "삭제된 활동에는 메모를 연결할 수 없습니다."),
    END_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "A004", "종료일 미정이 아니면 종료일을 입력해야 합니다."),
    INVALID_ACTIVITY_PERIOD(HttpStatus.BAD_REQUEST, "A005", "시작일은 종료일보다 늦을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
