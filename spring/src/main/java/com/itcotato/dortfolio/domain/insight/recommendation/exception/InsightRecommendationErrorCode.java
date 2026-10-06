package com.itcotato.dortfolio.domain.insight.recommendation.exception;

import com.itcotato.dortfolio.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InsightRecommendationErrorCode implements ErrorCode {

    INSIGHT_RECOMMENDATION_CONFIGURATION_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR, "I010", "Insight 추천 서비스 설정을 확인해야 합니다."
    ),
    INSIGHT_RECOMMENDATION_REJECTED(
            HttpStatus.BAD_GATEWAY, "I011", "Insight 추천 요청을 처리하지 못했습니다."
    ),
    INSIGHT_RECOMMENDATION_OUTPUT_LIMIT(
            HttpStatus.BAD_GATEWAY, "I012", "Insight 추천 생성 상한에 도달했습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
