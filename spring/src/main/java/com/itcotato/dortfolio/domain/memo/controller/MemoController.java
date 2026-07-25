package com.itcotato.dortfolio.domain.memo.controller;

import com.itcotato.dortfolio.domain.memo.controller.docs.MemoControllerDocs;
import com.itcotato.dortfolio.domain.memo.dto.req.MemoCreateRequest;
import com.itcotato.dortfolio.domain.memo.dto.res.MemoResponse;
import com.itcotato.dortfolio.domain.memo.dto.req.MemoUpdateRequest;
import com.itcotato.dortfolio.domain.memo.service.MemoService;
import com.itcotato.dortfolio.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// TODO: auth 도메인 완성되면 @RequestParam UUID userId를 @AuthenticationPrincipal로 교체
@RestController
@RequestMapping("/api/memos")
@RequiredArgsConstructor
public class MemoController implements MemoControllerDocs {

    private final MemoService memoService;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<UUID>> createMemo(
            @RequestParam UUID userId,
            @Valid @RequestBody MemoCreateRequest request
    ) {
        UUID memoId = memoService.createMemo(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("메모를 생성했습니다.", memoId));
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<List<MemoResponse>>> getMemos(@RequestParam UUID userId) {
        return ResponseEntity.ok(ApiResponse.success("메모 목록을 조회했습니다.", memoService.getMemos(userId)));
    }

    @Override
    @GetMapping("/{memoId}")
    public ResponseEntity<ApiResponse<MemoResponse>> getMemo(
            @RequestParam UUID userId,
            @PathVariable UUID memoId
    ) {
        return ResponseEntity.ok(ApiResponse.success("메모를 조회했습니다.", memoService.getMemo(userId, memoId)));
    }

    @Override
    @PatchMapping("/{memoId}")
    public ResponseEntity<ApiResponse<Void>> updateMemo(
            @RequestParam UUID userId,
            @PathVariable UUID memoId,
            @Valid @RequestBody MemoUpdateRequest request
    ) {
        memoService.updateMemo(userId, memoId, request);
        return ResponseEntity.ok(ApiResponse.success("메모를 수정했습니다."));
    }

    @Override
    @PatchMapping("/{memoId}/important")
    public ResponseEntity<ApiResponse<Void>> markImportant(
            @RequestParam UUID userId,
            @PathVariable UUID memoId,
            @RequestParam boolean important
    ) {
        memoService.markImportant(userId, memoId, important);
        String message = important ? "중요한 메모로 등록했습니다." : "중요한 메모 등록을 취소했습니다.";
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    @Override
    @DeleteMapping("/{memoId}")
    public ResponseEntity<ApiResponse<Void>> deleteMemo(
            @RequestParam UUID userId,
            @PathVariable UUID memoId
    ) {
        memoService.deleteMemo(userId, memoId);
        return ResponseEntity.ok(ApiResponse.success("메모를 삭제했습니다."));
    }
}
