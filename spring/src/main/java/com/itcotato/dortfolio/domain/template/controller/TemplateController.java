package com.itcotato.dortfolio.domain.template.controller;

import com.itcotato.dortfolio.domain.template.dto.TemplateCreateRequest;
import com.itcotato.dortfolio.domain.template.dto.TemplateResponse;
import com.itcotato.dortfolio.domain.template.dto.TemplateUpdateRequest;
import com.itcotato.dortfolio.domain.template.service.TemplateService;
import com.itcotato.dortfolio.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/templates")
public class TemplateController {

	private final TemplateService templateService;

	@GetMapping
	public ApiResponse<List<TemplateResponse>> getTemplates(@RequestHeader("X-User-Id") UUID userId) {
		return ApiResponse.success("템플릿 목록을 조회했습니다.", templateService.getTemplates(userId));
	}

	@GetMapping("/{templateId}")
	public ApiResponse<TemplateResponse> getTemplate(
		@RequestHeader("X-User-Id") UUID userId,
		@PathVariable UUID templateId
	) {
		return ApiResponse.success("템플릿을 조회했습니다.", templateService.getTemplate(userId, templateId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<TemplateResponse> createTemplate(
		@RequestHeader("X-User-Id") UUID userId,
		@Valid @RequestBody TemplateCreateRequest request
	) {
		return ApiResponse.success("템플릿을 등록했습니다.", templateService.createTemplate(userId, request));
	}

	@PatchMapping("/{templateId}")
	public ApiResponse<TemplateResponse> updateTemplate(
		@RequestHeader("X-User-Id") UUID userId,
		@PathVariable UUID templateId,
		@Valid @RequestBody TemplateUpdateRequest request
	) {
		return ApiResponse.success("템플릿을 수정했습니다.", templateService.updateTemplate(userId, templateId, request));
	}

	@DeleteMapping("/{templateId}")
	public ApiResponse<Void> deleteTemplate(
		@RequestHeader("X-User-Id") UUID userId,
		@PathVariable UUID templateId
	) {
		templateService.deleteTemplate(userId, templateId);
		return ApiResponse.success("템플릿을 삭제했습니다.");
	}
}
