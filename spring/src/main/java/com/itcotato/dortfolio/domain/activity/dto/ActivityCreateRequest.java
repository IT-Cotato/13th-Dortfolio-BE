package com.itcotato.dortfolio.domain.activity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record ActivityCreateRequest(
        @NotNull UUID activityTypeId,
        @NotBlank String title,
        String description,
        @NotNull LocalDate startedAt,
        LocalDate endedAt,
        boolean isOngoing
) {
}
