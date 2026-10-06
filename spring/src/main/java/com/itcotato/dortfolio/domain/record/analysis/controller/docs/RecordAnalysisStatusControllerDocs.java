package com.itcotato.dortfolio.domain.record.analysis.controller.docs;

import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisStatusResponse;
import com.itcotato.dortfolio.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Record", description = "기록 API")
public interface RecordAnalysisStatusControllerDocs {
	@Operation(summary = "기록 AI 분석 상태 조회", description = "본인 기록의 대기·실행·완료·실패 상태와 실패 시 재시도 가능 여부를 조회합니다.")
	ResponseEntity<ApiResponse<RecordAnalysisStatusResponse>> getStatus(
			@Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
			@Parameter(description = "기록 ID") @PathVariable UUID recordId
	);
}
