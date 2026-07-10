package com.itcotato.dortfolio.activity.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ActivityCreateRequest(
        UUID activityTypeId,
        String title,
        String description,
        LocalDate startedAt,
        LocalDate endedAt,
        boolean isOngoing
) {
}
