package com.itcotato.dortfolio.domain.matching.controller.docs;

import com.itcotato.dortfolio.domain.matching.dto.req.RecordMatchingRequest;
import com.itcotato.dortfolio.domain.matching.dto.res.MatchingQuestionTagsResponse;
import com.itcotato.dortfolio.domain.matching.dto.res.RecordMatchingResponse;
import com.itcotato.dortfolio.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "AI 기록 매칭", description = "자기소개서 문항 기반 기록 추천 API입니다.")
public interface MatchingControllerDocs {

	@Operation(summary = "추천 문항 태그 조회", description = "AI 기록 매칭 화면에서 사용할 기본 제공 자기소개서 문항 태그를 조회합니다.")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
	})
	ResponseEntity<ApiResponse<MatchingQuestionTagsResponse>> getQuestionTags();

	@Operation(
		summary = "AI 기록 매칭 실행",
		description = "자기소개서 문항과 관련성이 높은 완료 기록을 추천합니다. FastAPI에는 문항 임베딩 생성만 요청합니다."
	)
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "매칭 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "MCH001/MCH002/MCH003: 올바르지 않은 매칭 요청"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "MCH005: 일일 AI 기록 매칭 횟수 초과"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "MCH004/MCH006: AI 매칭 서비스 사용 불가 또는 응답 오류")
	})
	ResponseEntity<ApiResponse<RecordMatchingResponse>> matchRecords(
		@Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
		@RequestBody RecordMatchingRequest request
	);
}
