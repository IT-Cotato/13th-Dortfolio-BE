package com.itcotato.dortfolio.domain.memo.dto.res;

import com.itcotato.dortfolio.domain.memo.entity.MemoImage;
import java.util.UUID;

public record MemoImageResponse(
        UUID id,
        String imageUrl,
        int sortOrder
) {
    /**
     * 버킷이 비공개라 조회용 presigned URL을 매번 새로 발급해 내려준다.
     * s3Key가 없는 예전 데이터는 저장해둔 URL을 그대로 쓴다.
     */
    public static MemoImageResponse from(MemoImage memoImage, String downloadUrl) {
        return new MemoImageResponse(
                memoImage.getId(),
                downloadUrl != null ? downloadUrl : memoImage.getImageUrl(),
                memoImage.getSortOrder()
        );
    }
}
