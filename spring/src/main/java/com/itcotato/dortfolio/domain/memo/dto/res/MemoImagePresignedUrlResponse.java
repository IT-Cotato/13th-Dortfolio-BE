package com.itcotato.dortfolio.domain.memo.dto.res;

public record MemoImagePresignedUrlResponse(
        String presignedUrl,
        String s3Key
) {
}
