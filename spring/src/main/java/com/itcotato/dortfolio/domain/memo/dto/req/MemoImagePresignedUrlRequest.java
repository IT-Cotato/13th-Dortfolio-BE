package com.itcotato.dortfolio.domain.memo.dto.req;

import jakarta.validation.constraints.NotBlank;

public record MemoImagePresignedUrlRequest(
        @NotBlank(message = "파일명은 필수 입력 항목입니다.")
        String fileName
) {
}
