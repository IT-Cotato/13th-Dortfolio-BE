package com.itcotato.dortfolio.domain.activity.dto.req;

import jakarta.validation.constraints.NotBlank;

public record ActivityTypeCreateRequest(
        @NotBlank String name
) {
}
