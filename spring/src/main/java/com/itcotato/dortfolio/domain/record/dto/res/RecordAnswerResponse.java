package com.itcotato.dortfolio.domain.record.dto.res;

import com.itcotato.dortfolio.domain.record.entity.RecordAnswer;
import java.util.UUID;

public record RecordAnswerResponse(
	UUID templateQuestionId,
	String questionText,
	String questionDescription,
	boolean required,
	int sortOrder,
	String answerText
) {
	public static RecordAnswerResponse from(RecordAnswer answer) {
		var question = answer.getTemplateQuestion();
		return new RecordAnswerResponse(
			question.getId(),
			question.getQuestionText(),
			question.getDescription(),
			question.isRequired(),
			question.getSortOrder(),
			answer.getAnswerText()
		);
	}

}
