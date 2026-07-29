package com.itcotato.dortfolio.domain.memo.controller.docs;

import com.itcotato.dortfolio.domain.memo.dto.req.MemoCreateRequest;
import com.itcotato.dortfolio.domain.memo.dto.req.MemoImagePresignedUrlRequest;
import com.itcotato.dortfolio.domain.memo.dto.req.MemoUpdateRequest;
import com.itcotato.dortfolio.domain.memo.dto.res.MemoImagePresignedUrlResponse;
import com.itcotato.dortfolio.domain.memo.dto.res.MemoResponse;
import com.itcotato.dortfolio.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

@Tag(name = "메모 (Memo)", description = "빠른 메모 생성/조회/수정/삭제 API입니다.")
public interface MemoControllerDocs {

    @Operation(summary = "메모 생성", description = "내용(필수)과 제목/활동 태그/활동 사진(모두 선택)을 입력받아 새로운 메모를 생성합니다. 생성일로부터 30일 뒤 자동 삭제됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "메모 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "G001: 입력값 검증 실패 (내용 누락, 500자 초과 등) / A003: 삭제된 활동에 연결 시도"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "A001: 존재하지 않는 활동 / U001: 존재하지 않는 회원")
    })
    ResponseEntity<ApiResponse<UUID>> createMemo(UUID userId, MemoCreateRequest request);

    @Operation(summary = "메모 목록 조회", description = "내 메모 목록을 최신순으로 조회합니다. activityId를 지정하면 해당 활동 태그로 필터링하고, 지정하지 않으면 전체를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<ApiResponse<List<MemoResponse>>> getMemos(UUID userId, UUID activityId);

    @Operation(summary = "메모 상세 조회")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "M001: 존재하지 않는 메모")
    })
    ResponseEntity<ApiResponse<MemoResponse>> getMemo(UUID userId, UUID memoId);

    @Operation(summary = "메모 수정", description = "제목/내용/색상만 수정합니다. 활동 태그와 이미지는 생성 이후 수정하거나 새로 추가할 수 없습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "G001: 입력값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "M001: 존재하지 않는 메모")
    })
    ResponseEntity<ApiResponse<Void>> updateMemo(UUID userId, UUID memoId, MemoUpdateRequest request);

    @Operation(summary = "중요한 메모 등록/취소", description = "메모를 중요 메모로 등록하거나 등록을 취소합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "M001: 존재하지 않는 메모")
    })
    ResponseEntity<ApiResponse<Void>> markImportant(UUID userId, UUID memoId, boolean important);

    @Operation(summary = "메모 삭제 (단건/다건)",
            description = "메모 1개 이상을 한 번에 삭제합니다. 삭제 후에는 복구할 수 없으며, 연결된 이미지도 함께 삭제됩니다. "
                    + "기록에 연결된 메모는 삭제할 수 없고, 삭제 대상 중 하나라도 해당되면 전체 삭제가 실패합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "M004: 기록에 연결된 메모가 포함된 경우"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "M001: 존재하지 않는 메모가 포함된 경우")
    })
    ResponseEntity<ApiResponse<Void>> deleteMemos(UUID userId, List<UUID> memoIds);

    @Operation(summary = "활동 사진 업로드 Presigned URL 발급",
            description = "활동 사진 업로드용 Presigned URL을 발급합니다. JPG, PNG 확장자만 허용합니다. "
                    + "발급받은 URL로 S3에 직접 업로드한 뒤, 메모 생성 시 images에 imageUrl과 s3Key를 함께 담아 전달하세요.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "M003: JPG, PNG 외 확장자")
    })
    ResponseEntity<ApiResponse<MemoImagePresignedUrlResponse>> getMemoImagePresignedUrl(
            UUID userId, MemoImagePresignedUrlRequest request);

    @Operation(summary = "활동 사진 삭제", description = "메모에 업로드된 활동 사진을 삭제합니다. 메타데이터와 S3 객체를 함께 제거합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "M002: 존재하지 않는 메모 이미지")
    })
    ResponseEntity<ApiResponse<Void>> deleteMemoImage(UUID userId, UUID imageId);
}
