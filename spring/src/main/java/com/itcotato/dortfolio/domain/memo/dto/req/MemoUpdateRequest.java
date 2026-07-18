package com.itcotato.dortfolio.domain.memo.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record MemoUpdateRequest(
        UUID activityId,
        String title,
        @NotBlank @Size(max = 500) String content,
        String color
) {
}
