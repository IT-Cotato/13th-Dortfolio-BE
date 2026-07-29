package com.itcotato.dortfolio.domain.memo.dto.req;

import jakarta.validation.constraints.NotBlank;

// Presigned URL로 업로드를 마친 활동 사진 정보 (s3Key는 이후 S3 객체 삭제에 사용)
public record MemoImageRequest(
        @NotBlank String imageUrl,
        @NotBlank String s3Key
) {
}
