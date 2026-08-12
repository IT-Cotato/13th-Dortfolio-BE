package com.itcotato.dortfolio.domain.mypage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileImagePresignedUrlRequest(
        @NotBlank(message = "파일명은 필수 입력 항목입니다.")
        @Size(max = 255, message = "파일명은 255자 이하이어야 합니다.")
        @Pattern(
                regexp = "(?i)^.+\\.(jpg|jpeg|png|webp)$",
                message = "프로필 이미지는 jpg, jpeg, png, webp 형식만 업로드할 수 있습니다."
        )
        String fileName
) {}
