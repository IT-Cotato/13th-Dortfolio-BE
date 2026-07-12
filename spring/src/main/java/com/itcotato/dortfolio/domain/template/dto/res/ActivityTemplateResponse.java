package com.itcotato.dortfolio.domain.template.dto.res;

import com.itcotato.dortfolio.domain.template.entity.Template;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record ActivityTemplateResponse(
	UUID id,
	String title,
	String description,
	boolean isBuiltin,
	boolean isSelectedForActivity,
	int sortOrder,
	int questionCount,
	List<TemplateQuestionResponse> questions
) {

	public static ActivityTemplateResponse of(Template template, int sortOrder) {
		List<TemplateQuestionResponse> questions = template.getQuestions().stream()
			.filter(question -> !question.isDeleted())
			.sorted(Comparator.comparingInt(question -> question.getSortOrder()))
			.map(TemplateQuestionResponse::from)
			.toList();

		return new ActivityTemplateResponse(
			template.getId(),
			template.getTitle(),
			template.getDescription(),
			template.isBuiltin(),
			true,
			sortOrder,
			questions.size(),
			questions
		);
	}
}
