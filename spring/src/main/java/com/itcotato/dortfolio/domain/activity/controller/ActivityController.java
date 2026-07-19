package com.itcotato.dortfolio.domain.activity.controller;

import com.itcotato.dortfolio.domain.activity.dto.ActivityCreateRequest;
import com.itcotato.dortfolio.domain.activity.dto.ActivityResponse;
import com.itcotato.dortfolio.domain.activity.dto.ActivityUpdateRequest;
import com.itcotato.dortfolio.domain.activity.service.ActivityService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @PostMapping
    public ResponseEntity<Void> createActivity(
            @RequestParam UUID userId,
            @Valid @RequestBody ActivityCreateRequest request
    ) {
        UUID activityId = activityService.createActivity(userId, request);
        return ResponseEntity.created(URI.create("/api/activities/" + activityId)).build();
    }

    @GetMapping
    public ResponseEntity<List<ActivityResponse>> getActivities(@RequestParam UUID userId) {
        return ResponseEntity.ok(activityService.getActivities(userId));
    }

    @PatchMapping("/{activityId}")
    public ResponseEntity<Void> updateActivity(
            @RequestParam UUID userId,
            @PathVariable UUID activityId,
            @Valid @RequestBody ActivityUpdateRequest request
    ) {
        activityService.updateActivity(userId, activityId, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{activityId}/archive")
    public ResponseEntity<Void> archiveActivity(
            @RequestParam UUID userId,
            @PathVariable UUID activityId
    ) {
        activityService.archiveActivity(userId, activityId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{activityId}")
    public ResponseEntity<Void> deleteActivity(
            @RequestParam UUID userId,
            @PathVariable UUID activityId
    ) {
        activityService.deleteActivity(userId, activityId);
        return ResponseEntity.noContent().build();
    }
}
