package com.itcotato.dortfolio.domain.record.controller;

import com.itcotato.dortfolio.domain.record.dto.req.RecordCreateRequest;
import com.itcotato.dortfolio.domain.record.dto.req.RecordUpdateRequest;
import com.itcotato.dortfolio.domain.record.dto.res.RecordResponse;
import com.itcotato.dortfolio.domain.record.dto.res.RecordSummaryResponse;
import com.itcotato.dortfolio.domain.record.entity.RecordStatus;
import com.itcotato.dortfolio.domain.record.service.RecordService;
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
@RequestMapping("/api")
public class RecordController {

	private final RecordService recordService;

	@PostMapping("/records")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<RecordResponse> createRecord(
		@RequestParam UUID userId,
		@Valid @RequestBody RecordCreateRequest request
	) {
		return ApiResponse.success("기록을 저장했습니다.", recordService.createRecord(userId, request));
	}

	@GetMapping("/records/{recordId}")
	public ApiResponse<RecordResponse> getRecord(
		@RequestParam UUID userId,
		@PathVariable UUID recordId
	) {
		return ApiResponse.success("기록을 조회했습니다.", recordService.getRecord(userId, recordId));
	}

	@PatchMapping("/records/{recordId}")
	public ApiResponse<RecordResponse> updateRecord(
		@RequestParam UUID userId,
		@PathVariable UUID recordId,
		@Valid @RequestBody RecordUpdateRequest request
	) {
		return ApiResponse.success("기록을 수정했습니다.", recordService.updateRecord(userId, recordId, request));
	}

	@GetMapping("/activities/{activityId}/records")
	public ApiResponse<List<RecordSummaryResponse>> getActivityRecords(
		@RequestParam UUID userId,
		@PathVariable UUID activityId
	) {
		return ApiResponse.success("활동의 기록 목록을 조회했습니다.", recordService.getActivityRecords(userId, activityId));
	}

	@GetMapping("/records")
	public ApiResponse<List<RecordSummaryResponse>> getRecords(
		@RequestParam UUID userId,
		@RequestParam(required = false) UUID activityId,
		@RequestParam(required = false) UUID templateId,
		@RequestParam(required = false) RecordStatus status
	) {
		return ApiResponse.success("기록 목록을 조회했습니다.", recordService.getRecords(userId, activityId, templateId, status));
	}

	@GetMapping("/records/recent")
	public ApiResponse<List<RecordSummaryResponse>> getRecentRecords(@RequestParam UUID userId) {
		return ApiResponse.success("최근 작성한 기록을 조회했습니다.", recordService.getRecentRecords(userId));
	}

	@DeleteMapping("/records/{recordId}")
	public ApiResponse<Void> deleteRecord(
		@RequestParam UUID userId,
		@PathVariable UUID recordId
	) {
		recordService.deleteRecord(userId, recordId);
		return ApiResponse.success("기록을 삭제했습니다.");
	}

	@PatchMapping("/records/{recordId}/restore")
	public ApiResponse<Void> restoreRecord(
		@RequestParam UUID userId,
		@PathVariable UUID recordId
	) {
		recordService.restoreRecord(userId, recordId);
		return ApiResponse.success("기록을 복구했습니다.");
	}
}
