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

@Component
@RequiredArgsConstructor
public class InsightGenerationRunningWriter {

    private final InsightRepository insightRepository;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRunning(UUID insightId) {
        Insight insight = insightRepository.findById(insightId)
                .orElseThrow(() -> new CustomException(
                        InsightErrorCode.INSIGHT_NOT_FOUND
                ));

        insight.start(LocalDateTime.now(clock));
    }
}
