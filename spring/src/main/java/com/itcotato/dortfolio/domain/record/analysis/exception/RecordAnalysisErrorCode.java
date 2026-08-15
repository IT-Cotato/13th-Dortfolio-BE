package com.itcotato.dortfolio.domain.record.analysis.exception;

import com.itcotato.dortfolio.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RecordAnalysisErrorCode implements ErrorCode {

	RECORD_ANALYSIS_AI_SERVICE_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "RA001", "AI 분석 서비스를 사용할 수 없습니다.", true),
	RECORD_ANALYSIS_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "RA002", "AI 분석 응답이 올바르지 않습니다.", false),
	RECORD_ANALYSIS_EVIDENCE_SERIALIZE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "RA003", "핵심 근거 문장 저장 형식 변환에 실패했습니다.", false),
	RECORD_ANALYSIS_AI_SERVICE_REJECTED(HttpStatus.BAD_GATEWAY, "RA004", "AI 분석 서비스가 요청을 처리하지 못했습니다.", false),
	RECORD_ANALYSIS_PERSISTENCE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "RA005", "AI 분석 결과 저장에 실패했습니다.", true),
    RECORD_ANALYSIS_PRIMARY_JOB_REQUIRED(HttpStatus.BAD_REQUEST, "RA006", "기록 분석을 이용하려면 주 희망 직무를 설정해야 합니다.", false),
    RECORD_ANALYSIS_JOB_COMPETENCIES_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, "RA007", "주 희망 직무의 핵심 역량 정보가 올바르지 않습니다.", false);

	private final HttpStatus status;
	private final String code;
	private final String message;
	private final boolean retryable;
}
