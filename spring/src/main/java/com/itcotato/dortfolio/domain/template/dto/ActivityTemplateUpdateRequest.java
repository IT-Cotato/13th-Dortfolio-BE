package com.itcotato.dortfolio.domain.template.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record ActivityTemplateUpdateRequest(
	@NotNull(message = "템플릿 목록은 필수입니다.")
	@Size(max = 4, message = "활동에 연결할 템플릿은 최대 4개까지 선택할 수 있습니다.")
	List<@NotNull(message = "템플릿 ID는 필수입니다.") UUID> templateIds
) {
}
