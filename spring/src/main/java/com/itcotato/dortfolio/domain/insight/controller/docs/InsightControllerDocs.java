package com.itcotato.dortfolio.domain.insight.controller.docs;

import com.itcotato.dortfolio.domain.insight.dto.res.InsightEligibilityApiResponse;
import com.itcotato.dortfolio.domain.insight.dto.res.InsightGenerationCreateResponse;
import com.itcotato.dortfolio.domain.insight.dto.res.InsightGenerationStatusResponse;
import com.itcotato.dortfolio.domain.insight.dto.res.LatestInsightResponse;
import com.itcotato.dortfolio.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(
        name = "Insight",
        description = "Insight 생성 가능 여부, 생성 요청 및 상태 조회 API"
)
public interface InsightControllerDocs {

    @Operation(
            summary = "Insight 생성 가능 여부 조회",
            description = """
                    인증 사용자의 희망 직무, 분석 완료 기록 수,
                    진행 중 Insight, 마지막 완료 시각 및 변경 사항을
                    기준으로 Insight 생성 가능 여부를 조회합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            )
    })
    ResponseEntity<ApiResponse<InsightEligibilityApiResponse>>
    getEligibility(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UUID userId
    );

    @Operation(
            summary = "Insight 생성 요청",
            description = """
                    인증 사용자의 현재 시각을 스냅샷 기준 시각으로
                    확정하고 Insight 생성을 비동기로 요청합니다.
                    요청이 접수되면 202 Accepted를 반환합니다.
                    이미 생성 중이면 기존 PENDING generation ID를
                    반환합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202",
                    description = "생성 요청 접수 또는 기존 PENDING 반환"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = """
                            I004: 생성 조건 미충족
                            I005: 생성 요청이 진행 중이나 기존 generation을
                            아직 조회할 수 없음
                            """
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "I007: 비동기 작업 제출 실패"
            )
    })
    ResponseEntity<ApiResponse<InsightGenerationCreateResponse>>
    createInsight(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UUID userId
    );

    @Operation(
            summary = "Insight 생성 상태 조회",
            description = """
                    인증 사용자 본인의 generation 상태를 조회합니다.
                    다른 사용자의 generation ID는 조회할 수 없습니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "I006: 존재하지 않거나 접근할 수 없는 generation"
            )
    })
    ResponseEntity<ApiResponse<InsightGenerationStatusResponse>>
    getGenerationStatus(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UUID userId,

            @Parameter(
                    description = "Insight generation ID",
                    required = true
            )
            @PathVariable UUID generationId
    );
    @Operation(
            summary = "최신 완료 Insight 조회",
            description = """
                인증 사용자의 가장 최근 COMPLETED Insight를 조회합니다.

                최신 생성 요청이 PENDING 또는 FAILED여도 이전에 완료된
                Insight를 반환합니다.

                응답에는 생성 당시 직무와 기록 스냅샷, 강점 TOP 5,
                템플릿 TOP 4, 직무 역량별 추천 기록이 포함됩니다.

                현재 PENDING generation이 있으면 currentGeneration에
                함께 반환합니다.

                changes에는 마지막 완료 Insight 이후 신규 완료 기록 수,
                희망 직무 변경 여부, 분석 진행 중 및 실패 기록 수를 반환합니다.
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "최신 완료 Insight 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "I006: 완료된 Insight가 존재하지 않음"
            )
    })
    ResponseEntity<ApiResponse<LatestInsightResponse>>
    getLatestInsight(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UUID userId
    );
}
