package com.itcotato.dortfolio.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponse (
        @Schema(description = "인증 방식", example = "Bearer")
        String grantType,

        @Schema(description = "API 인증에 사용하는 Access Token")
        String accessToken
) {
    public static TokenResponse of(String accessToken) {
        return new TokenResponse("Bearer", accessToken);
    }
}
