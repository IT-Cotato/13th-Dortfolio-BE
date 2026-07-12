package com.itcotato.dortfolio.domain.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.itcotato.dortfolio.domain.template.dto.TemplateCreateRequest;
import com.itcotato.dortfolio.domain.template.dto.TemplateQuestionRequest;
import com.itcotato.dortfolio.domain.template.dto.TemplateResponse;
import com.itcotato.dortfolio.domain.template.dto.TemplateUpdateRequest;
import com.itcotato.dortfolio.domain.template.entity.Template;
import com.itcotato.dortfolio.domain.template.repository.ActivityTemplateRepository;
import com.itcotato.dortfolio.domain.template.repository.TemplateRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.ErrorCode;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class TemplateServiceTest {

	@Autowired
	private TemplateService templateService;

	@Autowired
	private TemplateRepository templateRepository;

	@Autowired
	private ActivityTemplateRepository activityTemplateRepository;

	@BeforeEach
	void setUp() {
		activityTemplateRepository.deleteAll();
		templateRepository.deleteAll();
	}

	@Test
	void createTemplate() {
		UUID userId = UUID.randomUUID();
		TemplateCreateRequest request = new TemplateCreateRequest(
			"커스텀",
			"설명",
			List.of(new TemplateQuestionRequest("질문", "질문 설명", true))
		);

		TemplateResponse response = templateService.createTemplate(userId, request);

		assertThat(response.title()).isEqualTo("커스텀");
		assertThat(response.isBuiltin()).isFalse();
		assertThat(response.questions()).hasSize(1);
		assertThat(response.questions().get(0).sortOrder()).isEqualTo(1);
	}

	@Test
	void updateTemplate() {
		UUID userId = UUID.randomUUID();
		TemplateResponse created = templateService.createTemplate(userId, new TemplateCreateRequest(
			"수정 전",
			null,
			List.of(new TemplateQuestionRequest("질문1", null, true))
		));

		TemplateResponse updated = templateService.updateTemplate(userId, created.id(), new TemplateUpdateRequest(
			"수정 후",
			"수정 설명",
			List.of(
				new TemplateQuestionRequest("질문1", null, true),
				new TemplateQuestionRequest("질문2", "설명2", false)
			)
		));

		assertThat(updated.title()).isEqualTo("수정 후");
		assertThat(updated.questions()).hasSize(2);
		assertThat(updated.questions().get(1).sortOrder()).isEqualTo(2);
	}

	@Test
	void deleteTemplateExcludesFromList() {
		UUID userId = UUID.randomUUID();
		TemplateResponse created = templateService.createTemplate(userId, new TemplateCreateRequest(
			"삭제 대상",
			null,
			List.of(new TemplateQuestionRequest("질문", null, true))
		));

		templateService.deleteTemplate(userId, created.id());

		assertThat(templateService.getTemplates(userId))
			.extracting(TemplateResponse::id)
			.doesNotContain(created.id());
	}

	@Test
	void builtinTemplateCannotBeUpdatedOrDeleted() {
		UUID userId = UUID.randomUUID();
		Template builtin = Template.createBuiltin("BASIC", 1, "기본", "기본 설명");
		builtin.addQuestion(TemplateQuestionRequestFixture.requiredQuestion("질문"));
		Template saved = templateRepository.save(builtin);

		TemplateUpdateRequest request = new TemplateUpdateRequest(
			"수정",
			null,
			List.of(new TemplateQuestionRequest("질문", null, true))
		);

		assertThatThrownBy(() -> templateService.updateTemplate(userId, saved.getId(), request))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.BUILTIN_TEMPLATE_MODIFICATION_NOT_ALLOWED);
		assertThatThrownBy(() -> templateService.deleteTemplate(userId, saved.getId()))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.BUILTIN_TEMPLATE_MODIFICATION_NOT_ALLOWED);
	}

	private static class TemplateQuestionRequestFixture {

		private static com.itcotato.dortfolio.domain.template.entity.TemplateQuestion requiredQuestion(String text) {
			return com.itcotato.dortfolio.domain.template.entity.TemplateQuestion.create(text, null, true, 1);
		}
	}
}
