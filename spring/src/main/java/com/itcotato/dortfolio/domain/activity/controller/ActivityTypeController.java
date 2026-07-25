package com.itcotato.dortfolio.domain.activity.controller;

import com.itcotato.dortfolio.domain.activity.controller.docs.ActivityTypeControllerDocs;
import com.itcotato.dortfolio.domain.activity.dto.req.ActivityTypeCreateRequest;
import com.itcotato.dortfolio.domain.activity.dto.res.ActivityTypeResponse;
import com.itcotato.dortfolio.domain.activity.service.ActivityTypeService;
import com.itcotato.dortfolio.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// TODO: auth 도메인 완성되면 @RequestParam UUID userId를 @AuthenticationPrincipal로 교체
@RestController
@RequestMapping("/api/activity-types")
@RequiredArgsConstructor
public class ActivityTypeController implements ActivityTypeControllerDocs {

    private final ActivityTypeService activityTypeService;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<UUID>> createActivityType(
            @RequestParam UUID userId,
            @Valid @RequestBody ActivityTypeCreateRequest request
    ) {
        UUID activityTypeId = activityTypeService.createActivityType(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("활동 종류를 생성했습니다.", activityTypeId));
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<List<ActivityTypeResponse>>> getActivityTypes(@RequestParam UUID userId) {
        return ResponseEntity
                .ok(ApiResponse.success("활동 종류 목록을 조회했습니다.", activityTypeService.getActivityTypes(userId)));
    }
}
