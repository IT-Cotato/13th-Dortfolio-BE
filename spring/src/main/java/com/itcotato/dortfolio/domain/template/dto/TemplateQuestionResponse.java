package com.itcotato.dortfolio.domain.template.dto;

import com.itcotato.dortfolio.domain.template.entity.TemplateQuestion;
import java.util.UUID;

public record TemplateQuestionResponse(
	UUID id,
	String questionText,
	String description,
	boolean required,
	int sortOrder
) {

	public static TemplateQuestionResponse from(TemplateQuestion question) {
		return new TemplateQuestionResponse(
			question.getId(),
			question.getQuestionText(),
			question.getDescription(),
			question.isRequired(),
			question.getSortOrder()
		);
	}
}
