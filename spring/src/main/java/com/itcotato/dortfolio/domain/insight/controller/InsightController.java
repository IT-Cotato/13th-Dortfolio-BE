package com.itcotato.dortfolio.domain.insight.controller;

import com.itcotato.dortfolio.domain.insight.controller.docs.InsightControllerDocs;
import com.itcotato.dortfolio.domain.insight.dto.res.InsightEligibilityApiResponse;
import com.itcotato.dortfolio.domain.insight.dto.res.InsightGenerationCreateResponse;
import com.itcotato.dortfolio.domain.insight.dto.res.InsightGenerationStatusResponse;
import com.itcotato.dortfolio.domain.insight.generation.model.InsightGenerationStartResult;
import com.itcotato.dortfolio.domain.insight.generation.service.InsightGenerationService;
import com.itcotato.dortfolio.domain.insight.service.InsightEligibilityService;
import com.itcotato.dortfolio.domain.insight.service.InsightGenerationQueryService;
import com.itcotato.dortfolio.global.response.ApiResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/insights")
public class InsightController
        implements InsightControllerDocs {

    private final InsightEligibilityService eligibilityService;
    private final InsightGenerationService generationService;
    private final InsightGenerationQueryService generationQueryService;

    @Override
    @GetMapping("/eligibility")
    public ResponseEntity<ApiResponse<InsightEligibilityApiResponse>>
    getEligibility(
            @AuthenticationPrincipal UUID userId
    ) {
        InsightEligibilityApiResponse response =
                InsightEligibilityApiResponse.from(
                        eligibilityService.check(userId)
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Insight 생성 가능 여부를 조회했습니다.",
                        response
                )
        );
    }

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<InsightGenerationCreateResponse>>
    createInsight(
            @AuthenticationPrincipal UUID userId
    ) {
        InsightGenerationStartResult result =
                generationService.requestGeneration(userId);

        InsightGenerationCreateResponse response =
                new InsightGenerationCreateResponse(
                        result.generationId(),
                        result.status()
                );

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(
                        "Insight 생성을 요청했습니다.",
                        response
                ));
    }

    @Override
    @GetMapping("/generations/{generationId}")
    public ResponseEntity<ApiResponse<InsightGenerationStatusResponse>>
    getGenerationStatus(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID generationId
    ) {
        InsightGenerationStatusResponse response =
                generationQueryService.getStatus(
                        userId,
                        generationId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Insight 생성 상태를 조회했습니다.",
                        response
                )
        );
    }
}