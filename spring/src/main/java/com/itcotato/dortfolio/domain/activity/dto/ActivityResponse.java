package com.itcotato.dortfolio.domain.activity.dto;

import com.itcotato.dortfolio.domain.activity.entity.Activity;
import java.time.LocalDate;
import java.util.UUID;

public record ActivityResponse(
        UUID id,
        UUID activityTypeId,
        String activityTypeName,
        String title,
        String description,
        LocalDate startedAt,
        LocalDate endedAt,
        boolean isOngoing,
        String status
) {
    public static ActivityResponse from(Activity activity) {
        return new ActivityResponse(
                activity.getId(),
                activity.getActivityType().getId(),
                activity.getActivityType().getName(),
                activity.getTitle(),
                activity.getDescription(),
                activity.getStartedAt(),
                activity.getEndedAt(),
                activity.isOngoing(),
                activity.getStatus().name()
        );
    }
}
