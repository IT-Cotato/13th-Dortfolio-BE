package com.itcotato.dortfolio.domain.insight.recommendation.service;

import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.InsightErrorCode;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class InsightRecommendationRetrySleeper {

    public void sleep(Duration duration) {
        if (duration.isZero()) {
            return;
        }

        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CustomException(
                    InsightErrorCode.INSIGHT_RECOMMENDATION_AI_SERVICE_FAILED
            );
        }
    }
}
