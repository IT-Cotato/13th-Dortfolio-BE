package com.itcotato.dortfolio.domain.activity.service;

import com.itcotato.dortfolio.domain.activity.dto.req.ActivityCreateRequest;
import com.itcotato.dortfolio.domain.activity.dto.res.ActivityResponse;
import com.itcotato.dortfolio.domain.activity.dto.req.ActivityUpdateRequest;
import com.itcotato.dortfolio.domain.activity.entity.Activity;
import com.itcotato.dortfolio.domain.activity.entity.ActivityType;
import com.itcotato.dortfolio.domain.activity.repository.ActivityRecordQueryRepository;
import com.itcotato.dortfolio.domain.activity.repository.ActivityRepository;
import com.itcotato.dortfolio.domain.activity.repository.ActivityTypeRepository;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.ActivityErrorCode;
import com.itcotato.dortfolio.global.exception.types.UserErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityService {

    // 실행취소 스낵바가 사라지면 프론트가 곧바로 복구를 포기하므로, 유예는 짧아도 된다.
    // 요청이 끊겨 되돌리지 못한 삭제를 정리하기 위한 안전망일 뿐이다
    private static final int DELETE_GRACE_PERIOD_DAYS = 1;

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

    /**
     * 활동을 삭제하면 그 안의 기록도 함께 감춰진다.
     *
     * 기록은 활동 없이 존재할 수 없어서(activity_id NOT NULL), 활동만 지우면
     * 갈 곳 없는 기록이 조회에 계속 남는다.
     */
    @Transactional
    public void deleteActivity(UUID userId, UUID activityId) {
        Activity activity = getActivityOrThrow(activityId, userId);
        activity.markDeleted(DELETE_GRACE_PERIOD_DAYS);

        activityRecordQueryRepository.findActiveRecords(activityId)
                .forEach(record -> record.markDeleted(DELETE_GRACE_PERIOD_DAYS));
    }

    /**
     * 활동과 함께 감춰졌던 기록만 되살린다.
     *
     * 사용자가 그 전에 따로 지웠던 기록까지 살아나면 안 되므로, 활동의 삭제 시각을 기준으로 구분한다.
     */
    @Transactional
    public void restoreActivity(UUID userId, UUID activityId) {
        Activity activity = getActivityOrThrow(activityId, userId);
        LocalDateTime activityDeletedAt = activity.getDeletedAt();

        activity.restore();

        if (activityDeletedAt == null) {
            return;
        }

        activityRecordQueryRepository.findRecordsDeletedWithActivity(activityId, activityDeletedAt)
                .forEach(Record::restore);
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
