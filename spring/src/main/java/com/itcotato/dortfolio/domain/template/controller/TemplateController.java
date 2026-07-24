package com.itcotato.dortfolio.domain.template.controller;

import com.itcotato.dortfolio.domain.template.controller.docs.TemplateControllerDocs;
import com.itcotato.dortfolio.domain.template.dto.req.TemplateCreateRequest;
import com.itcotato.dortfolio.domain.template.dto.res.TemplateResponse;
import com.itcotato.dortfolio.domain.template.dto.req.TemplateUpdateRequest;
import com.itcotato.dortfolio.domain.template.service.TemplateService;
import com.itcotato.dortfolio.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/templates")
public class TemplateController implements TemplateControllerDocs {

	private final TemplateService templateService;

	@GetMapping
	public ResponseEntity<ApiResponse<List<TemplateResponse>>> getTemplates(@AuthenticationPrincipal UUID userId) {
		return ResponseEntity.ok(ApiResponse.success("템플릿 목록을 조회했습니다.", templateService.getTemplates(userId)));
	}

	@GetMapping("/{templateId}")
	public ResponseEntity<ApiResponse<TemplateResponse>> getTemplate(
		@AuthenticationPrincipal UUID userId,
		@PathVariable UUID templateId
	) {
		return ResponseEntity.ok(ApiResponse.success("템플릿을 조회했습니다.", templateService.getTemplate(userId, templateId)));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<TemplateResponse>> createTemplate(
		@AuthenticationPrincipal UUID userId,
		@Valid @RequestBody TemplateCreateRequest request
	) {
		return ResponseEntity
			.status(HttpStatus.CREATED)
			.body(ApiResponse.success("템플릿을 등록했습니다.", templateService.createTemplate(userId, request)));
	}

	@PatchMapping("/{templateId}")
	public ResponseEntity<ApiResponse<TemplateResponse>> updateTemplate(
		@AuthenticationPrincipal UUID userId,
		@PathVariable UUID templateId,
		@Valid @RequestBody TemplateUpdateRequest request
	) {
		return ResponseEntity.ok(ApiResponse.success("템플릿을 수정했습니다.", templateService.updateTemplate(userId, templateId, request)));
	}

	@DeleteMapping("/{templateId}")
	public ResponseEntity<ApiResponse<Void>> deleteTemplate(
		@AuthenticationPrincipal UUID userId,
		@PathVariable UUID templateId
	) {
		templateService.deleteTemplate(userId, templateId);
		return ResponseEntity.ok(ApiResponse.success("템플릿을 삭제했습니다."));
	}
}
