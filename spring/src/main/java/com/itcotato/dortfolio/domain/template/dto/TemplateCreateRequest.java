package com.itcotato.dortfolio.domain.template.dto;

import com.itcotato.dortfolio.domain.template.entity.Template;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record TemplateCreateRequest(
	@NotBlank(message = "템플릿 제목은 필수입니다.")
	@Size(max = Template.TITLE_MAX_LENGTH, message = "템플릿 제목은 최대 20자까지 입력할 수 있습니다.")
	String title,

	@Size(max = Template.DESCRIPTION_MAX_LENGTH, message = "템플릿 설명은 최대 50자까지 입력할 수 있습니다.")
	String description,

	@NotEmpty(message = "템플릿 항목은 1개 이상이어야 합니다.")
	List<@Valid TemplateQuestionRequest> questions
) {
}
