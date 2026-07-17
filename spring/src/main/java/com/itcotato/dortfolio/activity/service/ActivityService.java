package com.itcotato.dortfolio.activity.service;

import com.itcotato.dortfolio.activity.dto.ActivityCreateRequest;
import com.itcotato.dortfolio.activity.dto.ActivityResponse;
import com.itcotato.dortfolio.activity.dto.ActivityUpdateRequest;
import com.itcotato.dortfolio.activity.entity.Activity;
import com.itcotato.dortfolio.activity.entity.ActivityType;
import com.itcotato.dortfolio.activity.repository.ActivityRepository;
import com.itcotato.dortfolio.activity.repository.ActivityTypeRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityService {

    private static final int DELETE_GRACE_PERIOD_DAYS = 30;

    private final ActivityRepository activityRepository;
    private final ActivityTypeRepository activityTypeRepository;

    @Transactional
    public UUID createActivity(UUID userId, ActivityCreateRequest request) {
        ActivityType activityType = getActivityTypeOrThrow(request.activityTypeId(), userId);

        Activity activity = Activity.create(
                userId,
                activityType,
                request.title(),
                request.description(),
                request.startedAt(),
                request.endedAt(),
                request.isOngoing()
        );

        return activityRepository.save(activity).getId();
    }

    public List<ActivityResponse> getActivities(UUID userId) {
        return activityRepository.findAllByUserIdAndDeletedAtIsNull(userId).stream()
                .map(ActivityResponse::from)
                .toList();
    }

    @Transactional
    public void updateActivity(UUID userId, UUID activityId, ActivityUpdateRequest request) {
        Activity activity = getActivityOrThrow(activityId, userId);
        ActivityType activityType = getActivityTypeOrThrow(request.activityTypeId(), userId);

        activity.update(
                activityType,
                request.title(),
                request.description(),
                request.startedAt(),
                request.endedAt(),
                request.isOngoing()
        );
    }

    @Transactional
    public void archiveActivity(UUID userId, UUID activityId) {
        getActivityOrThrow(activityId, userId).archive();
    }

    @Transactional
    public void deleteActivity(UUID userId, UUID activityId) {
        getActivityOrThrow(activityId, userId).markDeleted(DELETE_GRACE_PERIOD_DAYS);
    }

    @Transactional
    public void restoreActivity(UUID userId, UUID activityId) {
        getActivityOrThrow(activityId, userId).restore();
    }

    private Activity getActivityOrThrow(UUID activityId, UUID userId) {
        return activityRepository.findByIdAndUserId(activityId, userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 활동입니다."));
    }

    private ActivityType getActivityTypeOrThrow(UUID activityTypeId, UUID userId) {
        return activityTypeRepository.findByIdAndUserId(activityTypeId, userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 활동 종류입니다."));
    }
}
