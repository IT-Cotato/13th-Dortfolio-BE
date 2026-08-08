package com.itcotato.dortfolio.domain.matching.dto.res;

import com.itcotato.dortfolio.domain.record.entity.RecordAnswer;

public record MatchingRecordAnswerResponse(
	String questionText,
	String answerText
) {

	public static MatchingRecordAnswerResponse from(RecordAnswer answer) {
		return new MatchingRecordAnswerResponse(answer.getTemplateQuestion().getQuestionText(), answer.getAnswerText());
	}
}
