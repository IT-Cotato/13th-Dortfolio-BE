package com.itcotato.dortfolio.domain.record.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.itcotato.dortfolio.domain.record.analysis.config.RecordAnalysisProperties;
import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisRequest;
import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisRequest.ActivityPayload;
import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisRequest.AnswerPayload;
import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisRequest.MemoPayload;
import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisRequest.TemplatePayload;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecordEmbeddingTextBuilderTest {

	@Test
	void buildsLabeledEmbeddingText() {
		RecordEmbeddingTextBuilder builder = new RecordEmbeddingTextBuilder(properties(12_000));

		String result = builder.build(request());

		assertThat(result).isEqualTo("""
			활동 제목: 프로젝트
			활동 설명: 백엔드 개선
			기록 제목: 추천 알고리즘 개선
			템플릿: 문제 해결 경험
			질문: 무엇을 했나요?
			답변: 병목을 찾아 개선했습니다.
			메모 제목: 후속 작업
			메모 내용: 부하 테스트를 추가했습니다.
			""".strip());
	}

	@Test
	void truncatesEmbeddingTextAtConfiguredMaximum() {
		RecordEmbeddingTextBuilder builder = new RecordEmbeddingTextBuilder(properties(20));

		String result = builder.build(request());

		assertThat(result).hasSize(20);
		assertThat(result).startsWith("활동 제목: 프로젝트\n활동 설명:");
	}

	private RecordAnalysisRequest request() {
		return new RecordAnalysisRequest(
			UUID.randomUUID(),
			"추천 알고리즘 개선",
			new ActivityPayload("프로젝트", "백엔드 개선"),
			new TemplatePayload("문제 해결 경험"),
			List.of(new AnswerPayload("무엇을 했나요?", "병목을 찾아 개선했습니다.")),
			List.of(new MemoPayload("후속 작업", "부하 테스트를 추가했습니다.")),
			List.of(),
			2
		);
	}

	private RecordAnalysisProperties properties(int maximumCharacters) {
		return new RecordAnalysisProperties(
			true,
			1,
			1,
			10,
			Duration.ofSeconds(1),
			5,
			2,
			0.4,
			maximumCharacters
		);
	}
}
