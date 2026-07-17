package com.itcotato.dortfolio.activity.service;

import com.itcotato.dortfolio.activity.dto.ActivityTypeCreateRequest;
import com.itcotato.dortfolio.activity.dto.ActivityTypeResponse;
import com.itcotato.dortfolio.activity.entity.ActivityType;
import com.itcotato.dortfolio.activity.repository.ActivityTypeRepository;
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

    @Transactional
    public UUID createActivityType(UUID userId, ActivityTypeCreateRequest request) {
        ActivityType activityType = ActivityType.create(userId, request.name());
        return activityTypeRepository.save(activityType).getId();
    }

    public List<ActivityTypeResponse> getActivityTypes(UUID userId) {
        return activityTypeRepository.findAllByUserId(userId).stream()
                .map(ActivityTypeResponse::from)
                .toList();
    }
}
