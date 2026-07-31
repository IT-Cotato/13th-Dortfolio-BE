package com.itcotato.dortfolio.global.exception.types;

import com.itcotato.dortfolio.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SearchErrorCode implements ErrorCode {

    INVALID_PAGE_REQUEST(HttpStatus.BAD_REQUEST, "S001", "올바르지 않은 페이지 요청입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
