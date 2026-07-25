package com.itcotato.dortfolio.domain.activity.controller.docs;

import com.itcotato.dortfolio.domain.activity.dto.req.ActivityCreateRequest;
import com.itcotato.dortfolio.domain.activity.dto.res.ActivityResponse;
import com.itcotato.dortfolio.domain.activity.dto.req.ActivityUpdateRequest;
import com.itcotato.dortfolio.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

@Tag(name = "활동 (Activity)", description = "활동 생성/조회/수정/보관/삭제 API입니다.")
public interface ActivityControllerDocs {

    @Operation(summary = "활동 생성", description = "제목, 활동 종류, 기간을 입력받아 새로운 활동을 생성합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "활동 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "G001: 입력값 검증 실패 (제목 누락, 기간 오류 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "A002: 존재하지 않는 활동 종류")
    })
    ResponseEntity<ApiResponse<UUID>> createActivity(UUID userId, ActivityCreateRequest request);

    @Operation(summary = "활동 목록 조회", description = "삭제되지 않은 내 활동 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<ApiResponse<List<ActivityResponse>>> getActivities(UUID userId);

    @Operation(summary = "활동 수정", description = "활동의 제목, 활동 종류, 기간 등을 수정합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "G001: 입력값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "A001: 존재하지 않는 활동 / A002: 존재하지 않는 활동 종류")
    })
    ResponseEntity<ApiResponse<Void>> updateActivity(UUID userId, UUID activityId, ActivityUpdateRequest request);

    @Operation(summary = "활동 보관", description = "활동의 기록을 종료하고 보관 처리합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "보관 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "A001: 존재하지 않는 활동")
    })
    ResponseEntity<ApiResponse<Void>> archiveActivity(UUID userId, UUID activityId);

    @Operation(summary = "활동 삭제", description = "활동을 소프트 삭제합니다. 유예기간 내에는 복구할 수 있습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "A001: 존재하지 않는 활동")
    })
    ResponseEntity<ApiResponse<Void>> deleteActivity(UUID userId, UUID activityId);
}
