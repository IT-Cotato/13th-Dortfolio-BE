package com.itcotato.dortfolio.domain.activity.service;

import com.itcotato.dortfolio.domain.activity.dto.req.ActivityTypeCreateRequest;
import com.itcotato.dortfolio.domain.activity.dto.res.ActivityTypeResponse;
import com.itcotato.dortfolio.domain.activity.entity.ActivityType;
import com.itcotato.dortfolio.domain.activity.repository.ActivityTypeRepository;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityTypeService {

    private final ActivityTypeRepository activityTypeRepository;
    private final UserRepository userRepository;

    @Transactional
    public UUID createActivityType(UUID userId, ActivityTypeCreateRequest request) {
        User user = getUserOrThrow(userId);
        ActivityType activityType = ActivityType.create(user, request.name());
        return activityTypeRepository.save(activityType).getId();
    }

    public List<ActivityTypeResponse> getActivityTypes(UUID userId) {
        return activityTypeRepository.findAllByUser_Id(userId).stream()
                .map(ActivityTypeResponse::from)
                .toList();
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    }
}
