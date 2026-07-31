package com.itcotato.dortfolio.domain.activity.controller;

import com.itcotato.dortfolio.domain.activity.controller.docs.ActivityControllerDocs;
import com.itcotato.dortfolio.domain.activity.dto.req.ActivityCreateRequest;
import com.itcotato.dortfolio.domain.activity.dto.res.ActivityResponse;
import com.itcotato.dortfolio.domain.activity.dto.req.ActivityUpdateRequest;
import com.itcotato.dortfolio.domain.activity.service.ActivityService;
import com.itcotato.dortfolio.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController implements ActivityControllerDocs {

    private final ActivityService activityService;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<UUID>> createActivity(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ActivityCreateRequest request
    ) {
        UUID activityId = activityService.createActivity(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("활동을 생성했습니다.", activityId));
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<List<ActivityResponse>>> getActivities(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity
                .ok(ApiResponse.success("활동 목록을 조회했습니다.", activityService.getActivities(userId)));
    }

    @Override
    @PatchMapping("/{activityId}")
    public ResponseEntity<ApiResponse<Void>> updateActivity(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID activityId,
            @Valid @RequestBody ActivityUpdateRequest request
    ) {
        activityService.updateActivity(userId, activityId, request);
        return ResponseEntity.ok(ApiResponse.success("활동을 수정했습니다."));
    }

    @Override
    @PatchMapping("/{activityId}/archive")
    public ResponseEntity<ApiResponse<Void>> archiveActivity(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID activityId
    ) {
        activityService.archiveActivity(userId, activityId);
        return ResponseEntity.ok(ApiResponse.success("활동을 보관했습니다."));
    }

    @Override
    @DeleteMapping("/{activityId}")
    public ResponseEntity<ApiResponse<Void>> deleteActivity(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID activityId
    ) {
        activityService.deleteActivity(userId, activityId);
        return ResponseEntity.ok(ApiResponse.success("활동을 삭제했습니다."));
    }
}
