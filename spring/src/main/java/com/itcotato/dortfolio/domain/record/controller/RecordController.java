package com.itcotato.dortfolio.domain.record.controller;

import com.itcotato.dortfolio.domain.record.controller.docs.RecordControllerDocs;
import com.itcotato.dortfolio.domain.record.dto.req.RecordCreateRequest;
import com.itcotato.dortfolio.domain.record.dto.req.RecordUpdateRequest;
import com.itcotato.dortfolio.domain.record.dto.res.RecordPageResponse;
import com.itcotato.dortfolio.domain.record.dto.res.RecordResponse;
import com.itcotato.dortfolio.domain.record.dto.res.RecordSummaryResponse;
import com.itcotato.dortfolio.domain.record.entity.RecordStatus;
import com.itcotato.dortfolio.domain.record.service.RecordService;
import com.itcotato.dortfolio.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class RecordController implements RecordControllerDocs {

	private final RecordService recordService;

	@PostMapping("/records")
	public ResponseEntity<ApiResponse<RecordResponse>> createRecord(
		@AuthenticationPrincipal UUID userId,
		@Valid @RequestBody RecordCreateRequest request
	) {
		return ResponseEntity
			.status(HttpStatus.CREATED)
			.body(ApiResponse.success("기록을 저장했습니다.", recordService.createRecord(userId, request)));
	}

	@GetMapping("/records/{recordId}")
	public ResponseEntity<ApiResponse<RecordResponse>> getRecord(
		@AuthenticationPrincipal UUID userId,
		@PathVariable UUID recordId
	) {
		return ResponseEntity.ok(ApiResponse.success("기록을 조회했습니다.", recordService.getRecord(userId, recordId)));
	}

	@PatchMapping("/records/{recordId}")
	public ResponseEntity<ApiResponse<RecordResponse>> updateRecord(
		@AuthenticationPrincipal UUID userId,
		@PathVariable UUID recordId,
		@Valid @RequestBody RecordUpdateRequest request
	) {
		return ResponseEntity.ok(ApiResponse.success("기록을 수정했습니다.", recordService.updateRecord(userId, recordId, request)));
	}

	@GetMapping(value = "/records", params = {"!startDate", "!endDate"})
	public ResponseEntity<ApiResponse<RecordPageResponse>> getRecordPage(
		@AuthenticationPrincipal UUID userId,
		@RequestParam(required = false) UUID activityId,
		@RequestParam(required = false) UUID templateId,
		@RequestParam(required = false) RecordStatus status,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(required = false) Integer size
	) {
		return ResponseEntity.ok(ApiResponse.success(
			"기록 목록을 조회했습니다.",
			recordService.getRecordPage(userId, activityId, templateId, status, page, size)
		));
	}

	@GetMapping(value = "/records", params = {"startDate", "endDate"})
	public ResponseEntity<ApiResponse<List<RecordSummaryResponse>>> getRecordsByDateRange(
		@AuthenticationPrincipal UUID userId,
		@RequestParam(required = false) UUID activityId,
		@RequestParam(required = false) UUID templateId,
		@RequestParam(required = false) RecordStatus status,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
	) {
		return ResponseEntity.ok(ApiResponse.success(
			"기록 목록을 조회했습니다.",
			recordService.getRecords(userId, activityId, templateId, status, startDate, endDate)
		));
	}

	@GetMapping(value = "/records", params = {"startDate", "!endDate"})
	public ResponseEntity<ApiResponse<List<RecordSummaryResponse>>> getRecordsWithoutEndDate(
		@AuthenticationPrincipal UUID userId,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate
	) {
		return ResponseEntity.ok(ApiResponse.success(
			"기록 목록을 조회했습니다.",
			recordService.getRecords(userId, null, null, null, startDate, null)
		));
	}

	@GetMapping(value = "/records", params = {"!startDate", "endDate"})
	public ResponseEntity<ApiResponse<List<RecordSummaryResponse>>> getRecordsWithoutStartDate(
		@AuthenticationPrincipal UUID userId,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
	) {
		return ResponseEntity.ok(ApiResponse.success(
			"기록 목록을 조회했습니다.",
			recordService.getRecords(userId, null, null, null, null, endDate)
		));
	}

	@GetMapping("/records/recent")
	public ResponseEntity<ApiResponse<List<RecordSummaryResponse>>> getRecentRecords(@AuthenticationPrincipal UUID userId) {
		return ResponseEntity.ok(ApiResponse.success("최근 작성한 기록을 조회했습니다.", recordService.getRecentRecords(userId)));
	}

	@DeleteMapping("/records/{recordId}")
	public ResponseEntity<ApiResponse<Void>> deleteRecord(
		@AuthenticationPrincipal UUID userId,
		@PathVariable UUID recordId
	) {
		recordService.deleteRecord(userId, recordId);
		return ResponseEntity.ok(ApiResponse.success("기록을 삭제했습니다."));
	}

	@PatchMapping("/records/{recordId}/restore")
	public ResponseEntity<ApiResponse<Void>> restoreRecord(
		@AuthenticationPrincipal UUID userId,
		@PathVariable UUID recordId
	) {
		recordService.restoreRecord(userId, recordId);
		return ResponseEntity.ok(ApiResponse.success("기록을 복구했습니다."));
	}

	@DeleteMapping("/records/{recordId}/permanent")
	public ResponseEntity<ApiResponse<Void>> permanentlyDeleteRecord(
		@AuthenticationPrincipal UUID userId,
		@PathVariable UUID recordId
	) {
		recordService.permanentlyDeleteRecord(userId, recordId);
		return ResponseEntity.ok(ApiResponse.success("기록을 영구 삭제했습니다."));
	}
}
