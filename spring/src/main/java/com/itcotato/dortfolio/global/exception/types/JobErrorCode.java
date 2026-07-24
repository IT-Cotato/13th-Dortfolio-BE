package com.itcotato.dortfolio.global.exception.types;

import com.itcotato.dortfolio.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum JobErrorCode implements ErrorCode {

    JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "J001", "존재하지 않는 직무입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}