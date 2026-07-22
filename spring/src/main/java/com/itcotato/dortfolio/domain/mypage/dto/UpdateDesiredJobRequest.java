package com.itcotato.dortfolio.domain.mypage.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateDesiredJobRequest(
        @NotNull(message = "변경할 직무 ID는 필수 항목입니다.")
        Long jobId
) {
}