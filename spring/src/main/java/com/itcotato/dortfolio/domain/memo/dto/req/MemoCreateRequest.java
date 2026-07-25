package com.itcotato.dortfolio.domain.memo.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record MemoCreateRequest(
        UUID activityId,
        @Size(max = 255) String title,
        @NotBlank @Size(max = 500) String content,
        @Size(max = 255) String color,
        // presigned URL로 업로드 완료한 활동 사진 URL 목록 (선택)
        List<String> imageUrls
) {
}
