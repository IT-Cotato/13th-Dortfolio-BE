package com.itcotato.dortfolio.domain.record.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "실패 기록 AI 분석 재시도 요청 결과")
public record RecordAnalysisRetryResponse(
	@Schema(description = "재분석 작업을 요청한 기록 수", example = "2")
	int requestedCount
) {
}
