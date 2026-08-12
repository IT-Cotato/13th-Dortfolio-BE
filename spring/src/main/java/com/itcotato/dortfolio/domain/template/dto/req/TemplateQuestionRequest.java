package com.itcotato.dortfolio.domain.template.dto.req;

import com.itcotato.dortfolio.domain.template.entity.TemplateQuestion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TemplateQuestionRequest(
	@NotBlank(message = "항목 제목은 필수입니다.")
	@Size(max = TemplateQuestion.QUESTION_TEXT_MAX_LENGTH, message = "항목 제목은 최대 30자까지 입력할 수 있습니다.")
	String questionText,

	@Size(max = TemplateQuestion.DESCRIPTION_MAX_LENGTH, message = "항목 설명은 최대 255자까지 입력할 수 있습니다.")
	String description,

	@NotNull(message = "필수 답변 여부는 필수입니다.")
	Boolean required
) {
}
