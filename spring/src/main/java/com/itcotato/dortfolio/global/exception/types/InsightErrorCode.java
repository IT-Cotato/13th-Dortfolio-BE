package com.itcotato.dortfolio.global.exception.types;

import com.itcotato.dortfolio.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InsightErrorCode implements ErrorCode {

    INSIGHT_RECOMMENDATION_CANDIDATES_EMPTY(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "I001",
            "추천할 기록 후보가 없습니다."
    ),

    INSIGHT_RECOMMENDATION_AI_SERVICE_FAILED(
            HttpStatus.SERVICE_UNAVAILABLE,
            "I002",
            "Insight 추천 서비스를 사용할 수 없습니다."
    ),

    INSIGHT_RECOMMENDATION_INVALID_RESPONSE(
            HttpStatus.BAD_GATEWAY,
            "I003",
            "Insight 추천 응답이 올바르지 않습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
