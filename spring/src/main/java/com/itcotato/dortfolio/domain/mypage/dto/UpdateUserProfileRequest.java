package com.itcotato.dortfolio.domain.mypage.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @Size(min = 1, max = 50, message = "이름(닉네임)은 1자 이상 50자 이하이어야 합니다.")
        @Pattern(regexp = ".*\\S.*", message = "이름(닉네임)은 공백으로만 구성할 수 없습니다.")
        String name,

        @Size(max = 100, message = "이메일 전체 길이는 100자를 초과할 수 없습니다.")
        @Pattern(
                regexp = "^(?!\\.)(?!.*\\.\\.)[A-Za-z0-9._+\\-]{1,64}(?<!\\.)@[A-Za-z0-9\\-]+(\\.[A-Za-z0-9\\-]+)*\\.[A-Za-z]{2,}$",
                message = "올바른 이메일 형식으로 입력해주세요. 예: cotato@gmail.com"
        )
        String email,

        @Size(max = 255, message = "프로필 이미지 경로는 255자 이하이어야 합니다.")
        String profileImageUrl
) {
    @AssertTrue(message = "이름, 이메일 또는 프로필 이미지 중 하나 이상을 입력해야 합니다.")
    public boolean isUpdateRequested() {

        return name != null || email != null || profileImageUrl != null;
    }
}
