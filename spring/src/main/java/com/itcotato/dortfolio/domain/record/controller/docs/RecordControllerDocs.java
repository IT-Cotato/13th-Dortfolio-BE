package com.itcotato.dortfolio.domain.record.controller.docs;

import com.itcotato.dortfolio.domain.record.dto.req.RecordCreateRequest;
import com.itcotato.dortfolio.domain.record.dto.req.RecordUpdateRequest;
import com.itcotato.dortfolio.domain.record.dto.res.RecordPageResponse;
import com.itcotato.dortfolio.domain.record.dto.res.RecordAnalysisRetryResponse;
import com.itcotato.dortfolio.domain.record.dto.res.RecordResponse;
import com.itcotato.dortfolio.domain.record.dto.res.RecordSummaryResponse;
import com.itcotato.dortfolio.domain.record.entity.RecordStatus;
import com.itcotato.dortfolio.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Record", description = "기록 API")
public interface RecordControllerDocs {

	@Operation(summary = "기록 생성", description = "활동에 연결된 템플릿을 선택해 기록을 생성합니다. 답변과 연결 메모를 함께 저장할 수 있습니다.")
	ResponseEntity<ApiResponse<RecordResponse>> createRecord(
		@Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
		@Valid @RequestBody RecordCreateRequest request
	);

	@Operation(summary = "기록 상세 조회", description = "단일 기록의 기본 정보, 답변, 연결 메모를 조회합니다.")
	ResponseEntity<ApiResponse<RecordResponse>> getRecord(
		@Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
		@Parameter(description = "기록 ID") @PathVariable UUID recordId
	);

	@Operation(summary = "기록 수정", description = "기록 제목, 답변, 연결 메모, 상태를 수정합니다.")
	ResponseEntity<ApiResponse<RecordResponse>> updateRecord(
		@Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
		@Parameter(description = "기록 ID") @PathVariable UUID recordId,
		@Valid @RequestBody RecordUpdateRequest request
	);

	@Operation(
		operationId = "getRecords",
		summary = "기록 목록 조회",
		description = "startDate와 endDate를 함께 지정하면 생성일 기준 기간의 전체 기록을 페이지네이션 없이 조회합니다. "
			+ "날짜를 지정하지 않으면 페이지 단위로 조회하며, 활동, 템플릿, 기록 상태 필터를 함께 사용할 수 있습니다."
	)
	ResponseEntity<ApiResponse<RecordPageResponse>> getRecordPage(
		@Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
		@Parameter(description = "활동 ID") @RequestParam(required = false) UUID activityId,
		@Parameter(description = "템플릿 ID") @RequestParam(required = false) UUID templateId,
		@Parameter(description = "기록 상태") @RequestParam(required = false) RecordStatus status,
		@Parameter(description = "페이지 번호, 0부터 시작") @RequestParam(defaultValue = "0") int page,
		@Parameter(description = "페이지 크기. 기본값: 7개") @RequestParam(required = false) Integer size
	);

	@Operation(
		operationId = "getRecords",
		summary = "기록 목록 조회",
		description = "startDate와 endDate를 함께 지정하면 생성일 기준 기간의 전체 기록을 페이지네이션 없이 조회합니다. "
			+ "날짜를 지정하지 않으면 페이지 단위로 조회하며, 활동, 템플릿, 기록 상태 필터를 함께 사용할 수 있습니다."
	)
	ResponseEntity<ApiResponse<List<RecordSummaryResponse>>> getRecordsByDateRange(
		@Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
		@Parameter(description = "활동 ID") @RequestParam(required = false) UUID activityId,
		@Parameter(description = "템플릿 ID") @RequestParam(required = false) UUID templateId,
		@Parameter(description = "기록 상태") @RequestParam(required = false) RecordStatus status,
		@Parameter(description = "조회 시작일 (포함)", example = "2026-08-01", required = false)
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
		@Parameter(description = "조회 종료일 (포함)", example = "2026-08-31", required = false)
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
	);

	@Operation(summary = "최근 작성 기록 조회", description = "최근 작성한 기록을 최대 5개까지 최신 작성순으로 조회합니다.")
	ResponseEntity<ApiResponse<List<RecordSummaryResponse>>> getRecentRecords(
		@Parameter(hidden = true) @AuthenticationPrincipal UUID userId
	);

	@Operation(
		summary = "실패한 기록 AI 분석 재시도",
		description = "현재 사용자의 재시도 가능한 분석 실패 기록을 다시 분석하도록 요청합니다. 작업은 비동기로 처리됩니다."
	)
	ResponseEntity<ApiResponse<RecordAnalysisRetryResponse>> retryFailedAnalyses(
		@Parameter(hidden = true) @AuthenticationPrincipal UUID userId
	);

	@Operation(summary = "기록 삭제", description = "기록을 소프트 삭제하고 실행 취소 가능한 유예 기간을 시작합니다.")
	ResponseEntity<ApiResponse<Void>> deleteRecord(
		@Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
		@Parameter(description = "기록 ID") @PathVariable UUID recordId
	);

	@Operation(summary = "기록 복구", description = "삭제 유예 기간 안의 기록을 복구합니다.")
	ResponseEntity<ApiResponse<Void>> restoreRecord(
		@Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
		@Parameter(description = "기록 ID") @PathVariable UUID recordId
	);

	@Operation(summary = "기록 영구 삭제", description = "삭제된 기록과 기록 하위 데이터를 영구 삭제합니다.")
	ResponseEntity<ApiResponse<Void>> permanentlyDeleteRecord(
		@Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
		@Parameter(description = "기록 ID") @PathVariable UUID recordId
	);
}
