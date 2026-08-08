package com.itcotato.dortfolio.domain.insight.service;

import com.itcotato.dortfolio.domain.insight.dto.res.InsightGenerationStatusResponse;
import com.itcotato.dortfolio.domain.insight.entity.Insight;
import com.itcotato.dortfolio.domain.insight.repository.InsightRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.InsightErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InsightGenerationQueryService {

    private final InsightRepository insightRepository;

    /* generation ID와 인증 사용자 ID를 함께 조회 */
    public InsightGenerationStatusResponse getStatus(
            UUID userId,
            UUID generationId
    ) {
        Insight insight = insightRepository
                .findByIdAndUser_Id(
                        generationId,
                        userId
                )
                .orElseThrow(() -> new CustomException(
                        InsightErrorCode.INSIGHT_NOT_FOUND
                ));

        return InsightGenerationStatusResponse.from(insight);
    }
}