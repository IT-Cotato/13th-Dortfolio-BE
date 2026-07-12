package com.itcotato.dortfolio.domain.template.controller;

import com.itcotato.dortfolio.domain.template.dto.req.ActivityTemplateUpdateRequest;
import com.itcotato.dortfolio.domain.template.dto.res.ActivityTemplateResponse;
import com.itcotato.dortfolio.domain.template.service.ActivityTemplateService;
import com.itcotato.dortfolio.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// TODO: auth 도메인 완성되면 @RequestParam UUID userId를 @AuthenticationPrincipal로 교체
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/activities/{activityId}/templates")
public class ActivityTemplateController {

	private final ActivityTemplateService activityTemplateService;

	@GetMapping
	public ApiResponse<List<ActivityTemplateResponse>> getActivityTemplates(
		@RequestParam UUID userId,
		@PathVariable UUID activityId
	) {
		return ApiResponse.success(
			"활동에 선택된 템플릿 목록을 조회했습니다.",
			activityTemplateService.getActivityTemplates(userId, activityId)
		);
	}

	@PutMapping
	public ApiResponse<List<ActivityTemplateResponse>> updateActivityTemplates(
		@RequestParam UUID userId,
		@PathVariable UUID activityId,
		@Valid @RequestBody ActivityTemplateUpdateRequest request
	) {
		return ApiResponse.success(
			"활동에 사용할 템플릿을 저장했습니다.",
			activityTemplateService.updateActivityTemplates(userId, activityId, request)
		);
	}
}
