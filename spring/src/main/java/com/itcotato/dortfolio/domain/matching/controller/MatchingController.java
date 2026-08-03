package com.itcotato.dortfolio.domain.matching.controller;

import com.itcotato.dortfolio.domain.matching.controller.docs.MatchingControllerDocs;
import com.itcotato.dortfolio.domain.matching.dto.req.RecordMatchingRequest;
import com.itcotato.dortfolio.domain.matching.dto.res.MatchingQuestionTagsResponse;
import com.itcotato.dortfolio.domain.matching.dto.res.RecordMatchingResponse;
import com.itcotato.dortfolio.domain.matching.service.MatchingQuestionTagService;
import com.itcotato.dortfolio.domain.matching.service.MatchingService;
import com.itcotato.dortfolio.global.response.ApiResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/matching")
public class MatchingController implements MatchingControllerDocs {

	private final MatchingQuestionTagService matchingQuestionTagService;
	private final MatchingService matchingService;

	@Override
	@GetMapping("/question-tags")
	public ResponseEntity<ApiResponse<MatchingQuestionTagsResponse>> getQuestionTags() {
		return ResponseEntity.ok(ApiResponse.success(
			"추천 문항을 조회했습니다.",
			matchingQuestionTagService.getQuestionTags()
		));
	}

	@Override
	@PostMapping("/records")
	public ResponseEntity<ApiResponse<RecordMatchingResponse>> matchRecords(
		@AuthenticationPrincipal UUID userId,
		@RequestBody RecordMatchingRequest request
	) {
		return ResponseEntity.ok(ApiResponse.success(
			"AI 기록 매칭을 완료했습니다.",
			matchingService.matchRecords(userId, request)
		));
	}
}
