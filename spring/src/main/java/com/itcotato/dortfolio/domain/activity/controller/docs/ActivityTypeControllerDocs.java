package com.itcotato.dortfolio.domain.activity.controller.docs;

import com.itcotato.dortfolio.domain.activity.dto.req.ActivityTypeCreateRequest;
import com.itcotato.dortfolio.domain.activity.dto.res.ActivityTypeResponse;
import com.itcotato.dortfolio.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

@Tag(name = "활동 종류 (ActivityType)", description = "활동 종류(태그) 생성/조회 API입니다.")
public interface ActivityTypeControllerDocs {

    @Operation(summary = "활동 종류 생성", description = "'+' 버튼으로 사용자가 직접 추가하는 활동 종류 태그를 생성합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "활동 종류 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "G001: 입력값 검증 실패 (이름 누락)")
    })
    ResponseEntity<ApiResponse<UUID>> createActivityType(UUID userId, ActivityTypeCreateRequest request);

    @Operation(summary = "활동 종류 목록 조회", description = "내가 등록한 활동 종류(기본 + 커스텀) 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<ApiResponse<List<ActivityTypeResponse>>> getActivityTypes(UUID userId);
}
