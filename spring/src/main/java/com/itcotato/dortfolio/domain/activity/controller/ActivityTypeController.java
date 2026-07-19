package com.itcotato.dortfolio.domain.activity.controller;

import com.itcotato.dortfolio.domain.activity.dto.ActivityTypeCreateRequest;
import com.itcotato.dortfolio.domain.activity.dto.ActivityTypeResponse;
import com.itcotato.dortfolio.domain.activity.service.ActivityTypeService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
public class ActivityTypeController {

    private final ActivityTypeService activityTypeService;

    @PostMapping
    public ResponseEntity<Void> createActivityType(
            @RequestParam UUID userId,
            @Valid @RequestBody ActivityTypeCreateRequest request
    ) {
        UUID activityTypeId = activityTypeService.createActivityType(userId, request);
        return ResponseEntity.created(URI.create("/api/activity-types/" + activityTypeId)).build();
    }

    @GetMapping
    public ResponseEntity<List<ActivityTypeResponse>> getActivityTypes(@RequestParam UUID userId) {
        return ResponseEntity.ok(activityTypeService.getActivityTypes(userId));
    }
}
