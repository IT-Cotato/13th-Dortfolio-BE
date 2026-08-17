package com.itcotato.dortfolio.domain.record.analysis.service;

import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisRequest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class RecordEmbeddingTextBuilder {

	public String build(RecordAnalysisRequest request) {
		Stream<String> recordFields = Stream.of(
			request.activity().title(),
			request.activity().description(),
			request.title(),
			request.template().title()
		);
		Stream<String> answers = request.answers().stream()
			.flatMap(answer -> Stream.of(answer.questionText(), answer.answerText()));
		Stream<String> memos = request.memos().stream()
			.flatMap(memo -> Stream.of(memo.title(), memo.content()));

		return Stream.of(recordFields, answers, memos)
			.flatMap(stream -> stream)
			.filter(value -> value != null && !value.isBlank())
			.map(String::trim)
			.collect(Collectors.joining(" "));
	}
}
