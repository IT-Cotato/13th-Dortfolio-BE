package com.itcotato.dortfolio.domain.record.analysis.dto;

import com.itcotato.dortfolio.domain.record.analysis.entity.AiAnalysisStatus;
import com.itcotato.dortfolio.domain.record.analysis.entity.RecordAnalysis;
import com.itcotato.dortfolio.domain.record.analysis.entity.RecordAnalysisJob;
import com.itcotato.dortfolio.domain.record.analysis.entity.RecordAnalysisJobStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "기록 AI 분석 상태")
public record RecordAnalysisStatusResponse(
		UUID recordId,
		@Schema(description = "NOT_REQUESTED, PENDING, RUNNING, COMPLETED, FAILED 중 하나")
		String status,
		String failureCode,
		String failureMessage,
		boolean failureRetryable
) {
	public static RecordAnalysisStatusResponse of(UUID recordId, RecordAnalysis analysis, RecordAnalysisJob job) {
		if (analysis == null) {
			return new RecordAnalysisStatusResponse(recordId, "NOT_REQUESTED", null, null, false);
		}
		String status = analysis.getAiAnalysisStatus().name();
		if (analysis.getAiAnalysisStatus() == AiAnalysisStatus.PENDING && job != null
				&& job.getAnalysisGeneration() == analysis.getAnalysisGeneration()
				&& job.getStatus() == RecordAnalysisJobStatus.RUNNING) {
			status = "RUNNING";
		}
		String failure = analysis.getAiAnalysisStatus() == AiAnalysisStatus.FAILED ? analysis.getFailureReason() : null;
		int separator = failure == null ? -1 : failure.indexOf(' ');
		return new RecordAnalysisStatusResponse(recordId, status,
				separator < 0 ? failure : failure.substring(0, separator),
				separator < 0 ? null : failure.substring(separator + 1),
				analysis.getAiAnalysisStatus() == AiAnalysisStatus.FAILED && analysis.isFailureRetryable());
	}
}
