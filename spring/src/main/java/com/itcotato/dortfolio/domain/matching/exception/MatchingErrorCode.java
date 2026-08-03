package com.itcotato.dortfolio.domain.matching.exception;

import com.itcotato.dortfolio.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MatchingErrorCode implements ErrorCode {

	MATCHING_QUESTION_REQUIRED(HttpStatus.BAD_REQUEST, "MCH001", "자기소개서 문항을 입력해 주세요."),
	MATCHING_QUESTION_TOO_LONG(HttpStatus.BAD_REQUEST, "MCH002", "자기소개서 문항은 설정된 최대 길이 이하로 입력해 주세요."),
	MATCHING_INVALID_LIMIT(HttpStatus.BAD_REQUEST, "MCH003", "매칭 결과 개수가 올바르지 않습니다."),
	MATCHING_AI_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "MCH004", "AI 기록 매칭을 잠시 사용할 수 없습니다."),
	MATCHING_DAILY_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "MCH005", "오늘 사용할 수 있는 AI 기록 매칭 횟수를 모두 사용했습니다."),
	MATCHING_INVALID_AI_RESPONSE(HttpStatus.SERVICE_UNAVAILABLE, "MCH006", "AI 기록 매칭 응답이 올바르지 않습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
