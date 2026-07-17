package com.itcotato.dortfolio.domain.template.controller;

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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// TODO: auth 도메인 완성되면 @RequestParam UUID userId를 @AuthenticationPrincipal로 교체
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/templates")
public class TemplateController {

	private final TemplateService templateService;

	@GetMapping
	public ApiResponse<List<TemplateResponse>> getTemplates(@RequestParam UUID userId) {
		return ApiResponse.success("템플릿 목록을 조회했습니다.", templateService.getTemplates(userId));
	}

	@GetMapping("/{templateId}")
	public ApiResponse<TemplateResponse> getTemplate(
		@RequestParam UUID userId,
		@PathVariable UUID templateId
	) {
		return ApiResponse.success("템플릿을 조회했습니다.", templateService.getTemplate(userId, templateId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<TemplateResponse> createTemplate(
		@RequestParam UUID userId,
		@Valid @RequestBody TemplateCreateRequest request
	) {
		return ApiResponse.success("템플릿을 등록했습니다.", templateService.createTemplate(userId, request));
	}

	@PatchMapping("/{templateId}")
	public ApiResponse<TemplateResponse> updateTemplate(
		@RequestParam UUID userId,
		@PathVariable UUID templateId,
		@Valid @RequestBody TemplateUpdateRequest request
	) {
		return ApiResponse.success("템플릿을 수정했습니다.", templateService.updateTemplate(userId, templateId, request));
	}

	@DeleteMapping("/{templateId}")
	public ApiResponse<Void> deleteTemplate(
		@RequestParam UUID userId,
		@PathVariable UUID templateId
	) {
		templateService.deleteTemplate(userId, templateId);
		return ApiResponse.success("템플릿을 삭제했습니다.");
	}
}
