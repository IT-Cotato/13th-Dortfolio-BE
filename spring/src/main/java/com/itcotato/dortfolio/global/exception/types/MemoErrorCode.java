package com.itcotato.dortfolio.global.exception.types;

import com.itcotato.dortfolio.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemoErrorCode implements ErrorCode {

    MEMO_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "존재하지 않는 메모입니다."),
    MEMO_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "M002", "존재하지 않는 메모 이미지입니다."),
    UNSUPPORTED_IMAGE_EXTENSION(HttpStatus.BAD_REQUEST, "M003", "JPG, PNG 형식의 이미지만 업로드할 수 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
