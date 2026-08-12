package com.itcotato.dortfolio.domain.template.controller.docs;

import com.itcotato.dortfolio.domain.template.dto.req.TemplateCreateRequest;
import com.itcotato.dortfolio.domain.template.dto.res.TemplateResponse;
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

@Tag(name = "Template", description = "템플릿 API")
public interface TemplateControllerDocs {

	@Operation(summary = "템플릿 목록 조회", description = "기본 템플릿과 사용자가 생성한 커스텀 템플릿 목록을 조회합니다.")
	ResponseEntity<ApiResponse<List<TemplateResponse>>> getTemplates(
		@Parameter(hidden = true) @AuthenticationPrincipal UUID userId
	);

	@Operation(summary = "템플릿 상세 조회", description = "템플릿의 기본 정보와 질문 목록을 조회합니다.")
	ResponseEntity<ApiResponse<TemplateResponse>> getTemplate(
		@Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
		@Parameter(description = "템플릿 ID") @PathVariable UUID templateId
	);

	@Operation(summary = "커스텀 템플릿 생성", description = "사용자가 기록 작성에 사용할 커스텀 템플릿과 질문 목록을 생성합니다.")
	ResponseEntity<ApiResponse<TemplateResponse>> createTemplate(
		@Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
		@Valid @RequestBody TemplateCreateRequest request
	);

	@Operation(
			summary = "커스텀 템플릿 삭제",
			description = "사용자가 생성한 커스텀 템플릿을 목록과 새 기록 작성 대상에서 제외합니다. 기존 기록은 유지됩니다."
	)
	ResponseEntity<ApiResponse<Void>> deleteTemplate(
		@Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
		@Parameter(description = "템플릿 ID") @PathVariable UUID templateId
	);
}
