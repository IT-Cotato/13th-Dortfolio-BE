package com.itcotato.dortfolio.domain.record.analysis.service;

import com.itcotato.dortfolio.domain.record.analysis.config.RecordAnalysisProperties;
import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisRequest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecordEmbeddingTextBuilder {

	private final RecordAnalysisProperties properties;

	public String build(RecordAnalysisRequest request) {
		Stream<String> recordFields = Stream.of(
			labeled("활동 제목", request.activity().title()),
			labeled("활동 설명", request.activity().description()),
			labeled("기록 제목", request.title()),
			labeled("템플릿", request.template().title())
		);
		Stream<String> answers = request.answers().stream()
			.flatMap(answer -> Stream.of(
				labeled("질문", answer.questionText()),
				labeled("답변", answer.answerText())
			));
		Stream<String> memos = request.memos().stream()
			.flatMap(memo -> Stream.of(
				labeled("메모 제목", memo.title()),
				labeled("메모 내용", memo.content())
			));

		String text = Stream.of(recordFields, answers, memos)
			.flatMap(stream -> stream)
			.filter(value -> value != null)
			.collect(Collectors.joining("\n"));
		return text.substring(0, Math.min(text.length(), properties.embeddingMaxCharacters()));
	}

	private String labeled(String label, String value) {
		return value == null || value.isBlank() ? null : label + ": " + value.trim();
	}
}
