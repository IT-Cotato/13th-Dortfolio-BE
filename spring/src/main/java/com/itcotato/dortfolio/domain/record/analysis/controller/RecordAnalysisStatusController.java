package com.itcotato.dortfolio.domain.record.analysis.controller;

import com.itcotato.dortfolio.domain.record.analysis.controller.docs.RecordAnalysisStatusControllerDocs;
import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisStatusResponse;
import com.itcotato.dortfolio.domain.record.analysis.service.RecordAnalysisStatusQueryService;
import com.itcotato.dortfolio.global.response.ApiResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/records")
public class RecordAnalysisStatusController implements RecordAnalysisStatusControllerDocs {
	private final RecordAnalysisStatusQueryService queryService;

	@Override
	@GetMapping("/{recordId}/analysis")
	public ResponseEntity<ApiResponse<RecordAnalysisStatusResponse>> getStatus(
			@AuthenticationPrincipal UUID userId, @PathVariable UUID recordId
	) {
		return ResponseEntity.ok(ApiResponse.success("기록 AI 분석 상태를 조회했습니다.", queryService.getStatus(userId, recordId)));
	}
}
