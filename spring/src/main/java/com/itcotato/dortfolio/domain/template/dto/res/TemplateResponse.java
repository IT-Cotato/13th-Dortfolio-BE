package com.itcotato.dortfolio.domain.template.dto.res;

import com.itcotato.dortfolio.domain.template.entity.Template;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record TemplateResponse(
	UUID id,
	String title,
	String description,
	boolean isBuiltin,
	int questionCount,
	List<TemplateQuestionResponse> questions
) {

	public static TemplateResponse from(Template template) {
		List<TemplateQuestionResponse> questions = template.getQuestions().stream()
			.filter(question -> !question.isDeleted())
			.sorted(Comparator.comparingInt(question -> question.getSortOrder()))
			.map(TemplateQuestionResponse::from)
			.toList();

		return new TemplateResponse(
			template.getId(),
			template.getTitle(),
			template.getDescription(),
			template.isBuiltin(),
			questions.size(),
			questions
		);
	}
}
