package com.itcotato.dortfolio.domain.memo.dto.res;

import com.itcotato.dortfolio.domain.memo.entity.MemoImage;
import java.util.UUID;

public record MemoImageResponse(
        UUID id,
        String imageUrl,
        int sortOrder
) {
    public static MemoImageResponse from(MemoImage memoImage) {
        return new MemoImageResponse(
                memoImage.getId(),
                memoImage.getImageUrl(),
                memoImage.getSortOrder()
        );
    }
}
