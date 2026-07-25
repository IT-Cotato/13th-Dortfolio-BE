package com.itcotato.dortfolio.domain.memo.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 기능명세서 3.5.4: 메모 수정 시 제목/내용만 변경 가능하며 활동 태그는 수정/추가할 수 없음
public record MemoUpdateRequest(
        @Size(max = 255) String title,
        @NotBlank @Size(max = 500) String content,
        @Size(max = 255) String color
) {
}
