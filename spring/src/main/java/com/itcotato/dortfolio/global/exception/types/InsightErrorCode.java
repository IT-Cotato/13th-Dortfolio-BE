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
    ),

    INSIGHT_GENERATION_NOT_ALLOWED(
            HttpStatus.CONFLICT,
            "I004",
            "현재 Insight를 생성할 수 없습니다."
    ),

    INSIGHT_GENERATION_IN_PROGRESS(
            HttpStatus.CONFLICT,
            "I005",
            "Insight 생성이 이미 진행 중입니다."
    ),

    INSIGHT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "I006",
            "존재하지 않는 Insight입니다."
    ),

    INSIGHT_GENERATION_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "I007",
            "Insight 생성에 실패했습니다."
    ),

    INSIGHT_JOB_COMPETENCIES_NOT_READY(
            HttpStatus.CONFLICT,
            "I008",
            "직무 역량 정보가 준비되지 않았습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
