package com.itcotato.dortfolio.domain.mypage.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateDesiredJobRequest(
        @NotNull(message = "변경할 직무 ID는 필수 항목입니다.")
        UUID jobId
) {
}