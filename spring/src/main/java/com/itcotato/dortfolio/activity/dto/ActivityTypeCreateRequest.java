package com.itcotato.dortfolio.activity.dto;

import jakarta.validation.constraints.NotBlank;

public record ActivityTypeCreateRequest(
        @NotBlank String name
) {
}
