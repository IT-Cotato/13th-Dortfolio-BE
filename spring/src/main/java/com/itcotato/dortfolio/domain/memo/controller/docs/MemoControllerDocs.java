package com.itcotato.dortfolio.domain.memo.controller.docs;

import com.itcotato.dortfolio.domain.memo.dto.req.MemoCreateRequest;
import com.itcotato.dortfolio.domain.memo.dto.res.MemoResponse;
import com.itcotato.dortfolio.domain.memo.dto.req.MemoUpdateRequest;
import com.itcotato.dortfolio.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

@Tag(name = "메모 (Memo)", description = "빠른 메모 생성/조회/수정/삭제 API입니다.")
public interface MemoControllerDocs {

    @Operation(summary = "메모 생성", description = "내용(필수)과 제목/연결할 활동(둘 다 선택)을 입력받아 새로운 메모를 생성합니다. 생성일로부터 30일 뒤 자동 만료됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "메모 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "G001: 입력값 검증 실패 (내용 누락, 500자 초과 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "ACT001: activityId를 지정했지만 존재하지 않는 활동인 경우")
    })
    ResponseEntity<ApiResponse<UUID>> createMemo(UUID userId, MemoCreateRequest request);

    @Operation(summary = "메모 목록 조회", description = "삭제되지 않은 내 메모 목록을 최신순으로 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<ApiResponse<List<MemoResponse>>> getMemos(UUID userId);

    @Operation(summary = "메모 상세 조회")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "MEMO001: 존재하지 않는 메모")
    })
    ResponseEntity<ApiResponse<MemoResponse>> getMemo(UUID userId, UUID memoId);

    @Operation(summary = "메모 수정")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "G001: 입력값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "MEMO001: 존재하지 않는 메모 / ACT001: activityId를 지정했지만 존재하지 않는 활동인 경우")
    })
    ResponseEntity<ApiResponse<Void>> updateMemo(UUID userId, UUID memoId, MemoUpdateRequest request);

    @Operation(summary = "중요한 메모 등록/취소", description = "메모를 중요 메모로 등록하거나 등록을 취소합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "MEMO001: 존재하지 않는 메모")
    })
    ResponseEntity<ApiResponse<Void>> markImportant(UUID userId, UUID memoId, boolean important);

    @Operation(summary = "메모 삭제", description = "메모를 소프트 삭제합니다. 유예기간 내에는 복구할 수 있습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "MEMO001: 존재하지 않는 메모")
    })
    ResponseEntity<ApiResponse<Void>> deleteMemo(UUID userId, UUID memoId);
}
