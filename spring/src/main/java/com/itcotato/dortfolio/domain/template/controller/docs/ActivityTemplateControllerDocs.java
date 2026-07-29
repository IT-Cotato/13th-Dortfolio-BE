package com.itcotato.dortfolio.domain.template.controller.docs;

import com.itcotato.dortfolio.domain.template.dto.req.ActivityTemplateUpdateRequest;
import com.itcotato.dortfolio.domain.template.dto.res.ActivityTemplateResponse;
import com.itcotato.dortfolio.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Activity Template", description = "활동별 템플릿 연결 API")
public interface ActivityTemplateControllerDocs {

	@Operation(summary = "활동별 선택 템플릿 조회", description = "특정 활동에 연결되어 기록 작성에 사용할 수 있는 템플릿 목록을 조회합니다.")
	ResponseEntity<ApiResponse<List<ActivityTemplateResponse>>> getActivityTemplates(
		@Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
		@Parameter(description = "활동 ID") @PathVariable UUID activityId
	);

	@Operation(summary = "활동별 선택 템플릿 저장", description = "특정 활동에서 사용할 템플릿 목록과 정렬 순서를 저장합니다.")
	ResponseEntity<ApiResponse<List<ActivityTemplateResponse>>> updateActivityTemplates(
		@Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
		@Parameter(description = "활동 ID") @PathVariable UUID activityId,
		@Valid @RequestBody ActivityTemplateUpdateRequest request
	);
}
