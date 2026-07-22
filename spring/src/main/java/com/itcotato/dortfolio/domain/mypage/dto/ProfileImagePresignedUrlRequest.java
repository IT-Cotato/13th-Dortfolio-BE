package com.itcotato.dortfolio.domain.mypage.dto;

import jakarta.validation.constraints.NotBlank;

public record ProfileImagePresignedUrlRequest(
        @NotBlank(message = "파일명은 필수 입력 항목입니다.")
        String fileName
) {}