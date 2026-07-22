package com.itcotato.dortfolio.domain.mypage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @NotBlank(message = "이름(닉네임)은 필수 입력 항목입니다.")
        @Size(max = 50, message = "이름(닉네임)은 50자 이하이어야 합니다.")
        String name,

        String profileImageUrl
) {}
