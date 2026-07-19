package com.itcotato.dortfolio.domain.activity.dto;

import com.itcotato.dortfolio.domain.activity.entity.ActivityType;
import java.util.UUID;

public record ActivityTypeResponse(
        UUID id,
        String name,
        boolean isDefault
) {
    public static ActivityTypeResponse from(ActivityType activityType) {
        return new ActivityTypeResponse(
                activityType.getId(),
                activityType.getName(),
                activityType.isDefault()
        );
    }
}
