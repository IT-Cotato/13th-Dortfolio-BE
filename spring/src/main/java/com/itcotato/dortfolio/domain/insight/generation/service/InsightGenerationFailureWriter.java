package com.itcotato.dortfolio.domain.insight.generation.service;

import com.itcotato.dortfolio.domain.insight.entity.Insight;
import com.itcotato.dortfolio.domain.insight.repository.InsightRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.InsightErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/* Insight 생성 실패 상태를 별도 트랜잭션으로 저장 */
@Component
@RequiredArgsConstructor
public class InsightGenerationFailureWriter {

    private static final int MAX_FAILURE_CODE_LENGTH = 100;
    private static final int MAX_FAILURE_MESSAGE_LENGTH = 1_000;

    private final InsightRepository insightRepository;
    private final Clock clock;

    /* 기존 트랜잭션이 존재해도 중단하고 새로운 트랜잭션을 시작 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(
            UUID insightId,
            String failureCode,
            String failureMessage
    ) {
        Insight insight = insightRepository.findById(insightId)
                .orElseThrow(() -> new CustomException(
                        InsightErrorCode.INSIGHT_NOT_FOUND
                ));

        String normalizedCode = normalize(
                failureCode,
                "UNKNOWN_ERROR",
                MAX_FAILURE_CODE_LENGTH
        );

        String normalizedMessage = normalize(
                failureMessage,
                "Insight generation failed.",
                MAX_FAILURE_MESSAGE_LENGTH
        );

        insight.fail(
                LocalDateTime.now(clock),
                normalizedCode,
                normalizedMessage
        );
    }

    private String normalize(
            String value,
            String fallback,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        String normalized = value.strip();

        if (normalized.length() <= maxLength) {
            return normalized;
        }

        return normalized.substring(0, maxLength);
    }
}