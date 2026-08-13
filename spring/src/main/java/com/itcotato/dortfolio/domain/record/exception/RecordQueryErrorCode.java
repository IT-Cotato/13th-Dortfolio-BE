package com.itcotato.dortfolio.domain.record.exception;

import com.itcotato.dortfolio.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RecordQueryErrorCode implements ErrorCode {

	INVALID_DATE_RANGE(
		HttpStatus.BAD_REQUEST,
		"RECORD_017",
		"시작일과 종료일을 모두 입력하고 시작일이 종료일보다 늦지 않게 입력해주세요."
	);

	private final HttpStatus status;
	private final String code;
	private final String message;
}
