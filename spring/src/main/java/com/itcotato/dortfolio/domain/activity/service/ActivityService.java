package com.itcotato.dortfolio.domain.activity.service;

import com.itcotato.dortfolio.domain.activity.dto.req.ActivityCreateRequest;
import com.itcotato.dortfolio.domain.activity.dto.res.ActivityResponse;
import com.itcotato.dortfolio.domain.activity.dto.req.ActivityUpdateRequest;
import com.itcotato.dortfolio.domain.activity.entity.Activity;
import com.itcotato.dortfolio.domain.activity.entity.ActivityType;
import com.itcotato.dortfolio.domain.activity.repository.ActivityRepository;
import com.itcotato.dortfolio.domain.activity.repository.ActivityTypeRepository;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.ActivityErrorCode;
import com.itcotato.dortfolio.global.exception.types.UserErrorCode;
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
    private final UserRepository userRepository;

    @Transactional
    public UUID createActivity(UUID userId, ActivityCreateRequest request) {
        User user = getUserOrThrow(userId);
        ActivityType activityType = getActivityTypeOrThrow(request.activityTypeId(), userId);

        Activity activity = Activity.create(
                user,
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
        return activityRepository.findAllByUser_IdAndDeletedAtIsNull(userId).stream()
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
        return activityRepository.findByIdAndUser_Id(activityId, userId)
                .orElseThrow(() -> new CustomException(ActivityErrorCode.ACTIVITY_NOT_FOUND));
    }

    private ActivityType getActivityTypeOrThrow(UUID activityTypeId, UUID userId) {
        return activityTypeRepository.findByIdAndUser_Id(activityTypeId, userId)
                .orElseThrow(() -> new CustomException(ActivityErrorCode.ACTIVITY_TYPE_NOT_FOUND));
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }

}
