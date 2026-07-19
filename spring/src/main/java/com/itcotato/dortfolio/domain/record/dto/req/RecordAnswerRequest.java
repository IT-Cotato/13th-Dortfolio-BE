package com.itcotato.dortfolio.domain.record.dto.req;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RecordAnswerRequest(
	@NotNull(message = "질문 ID는 필수입니다.")
	UUID templateQuestionId,

	String answerText
) {
}
