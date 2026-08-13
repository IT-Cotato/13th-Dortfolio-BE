package com.itcotato.dortfolio.domain.memo.controller;

import com.itcotato.dortfolio.domain.memo.controller.docs.MemoControllerDocs;
import com.itcotato.dortfolio.domain.memo.dto.req.MemoCreateRequest;
import com.itcotato.dortfolio.domain.memo.dto.req.MemoImagePresignedUrlRequest;
import com.itcotato.dortfolio.domain.memo.dto.req.MemoUpdateRequest;
import com.itcotato.dortfolio.domain.memo.dto.res.MemoImagePresignedUrlResponse;
import com.itcotato.dortfolio.domain.memo.dto.res.MemoResponse;
import com.itcotato.dortfolio.domain.memo.service.MemoService;
import com.itcotato.dortfolio.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/memos")
@RequiredArgsConstructor
public class MemoController implements MemoControllerDocs {

    private final MemoService memoService;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<UUID>> createMemo(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody MemoCreateRequest request
    ) {
        UUID memoId = memoService.createMemo(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("메모를 생성했습니다.", memoId));
    }

    // 기능명세서 3.4: activityId 미지정 시 전체보기, 지정 시 해당 활동 태그로 필터링
    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<List<MemoResponse>>> getMemos(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(required = false) UUID activityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "메모 목록을 조회했습니다.",
                memoService.getMemos(userId, activityId, startDate, endDate)));
    }

    @Override
    @GetMapping("/{memoId}")
    public ResponseEntity<ApiResponse<MemoResponse>> getMemo(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID memoId
    ) {
        return ResponseEntity.ok(ApiResponse.success("메모를 조회했습니다.", memoService.getMemo(userId, memoId)));
    }

    @Override
    @PatchMapping("/{memoId}")
    public ResponseEntity<ApiResponse<Void>> updateMemo(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID memoId,
            @Valid @RequestBody MemoUpdateRequest request
    ) {
        memoService.updateMemo(userId, memoId, request);
        return ResponseEntity.ok(ApiResponse.success("메모를 수정했습니다."));
    }

    @Override
    @PatchMapping("/{memoId}/important")
    public ResponseEntity<ApiResponse<Void>> markImportant(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID memoId,
            @RequestParam boolean important
    ) {
        memoService.markImportant(userId, memoId, important);
        String message = important ? "중요한 메모로 등록했습니다." : "중요한 메모 등록을 취소했습니다.";
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    // 단건/다건(1~n개) 삭제 동일 API로 처리 (3.2.2, 3.3.3)
    @Override
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteMemos(
            @AuthenticationPrincipal UUID userId,
            @RequestParam List<UUID> memoIds
    ) {
        memoService.deleteMemos(userId, memoIds);
        return ResponseEntity.ok(ApiResponse.success("메모를 삭제했습니다."));
    }

    /* 삭제 실행취소 (3.2.3.1.1) */
    @Override
    @PatchMapping("/restore")
    public ResponseEntity<ApiResponse<Void>> restoreMemos(
            @AuthenticationPrincipal UUID userId,
            @RequestParam List<UUID> memoIds
    ) {
        memoService.restoreMemos(userId, memoIds);
        return ResponseEntity.ok(ApiResponse.success("메모를 복구했습니다."));
    }

    /* 활동 사진 업로드 Presigned URL 발급 API (3.1.3) */
    @Override
    @PostMapping("/images/presigned-url")
    public ResponseEntity<ApiResponse<MemoImagePresignedUrlResponse>> getMemoImagePresignedUrl(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody MemoImagePresignedUrlRequest request
    ) {
        MemoImagePresignedUrlResponse response = memoService.createMemoImagePresignedUrl(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Presigned URL 발급에 성공하였습니다.", response));
    }

    /* 활동 사진 삭제 API (3.1.4) */
    @Override
    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMemoImage(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID imageId
    ) {
        memoService.deleteMemoImage(userId, imageId);
        return ResponseEntity.ok(ApiResponse.success("이미지를 삭제했습니다."));
    }
}
