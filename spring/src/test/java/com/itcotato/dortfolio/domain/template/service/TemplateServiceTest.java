package com.itcotato.dortfolio.domain.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.itcotato.dortfolio.domain.template.dto.req.TemplateCreateRequest;
import com.itcotato.dortfolio.domain.template.dto.req.TemplateQuestionRequest;
import com.itcotato.dortfolio.domain.template.dto.res.TemplateResponse;
import com.itcotato.dortfolio.domain.template.config.BuiltinTemplateInitializer;
import com.itcotato.dortfolio.domain.template.entity.Template;
import com.itcotato.dortfolio.domain.template.entity.TemplateQuestion;
import com.itcotato.dortfolio.domain.activity.repository.ActivityRepository;
import com.itcotato.dortfolio.domain.activity.repository.ActivityTypeRepository;
import com.itcotato.dortfolio.domain.template.repository.TemplateRepository;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.TemplateErrorCode;

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
	private UserRepository userRepository;

	@Autowired
	private ActivityRepository activityRepository;

	@Autowired
	private ActivityTypeRepository activityTypeRepository;

	@BeforeEach
	void setUp() {
		templateRepository.deleteAll();
		activityRepository.deleteAll();
		activityTypeRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void createTemplate() {
		UUID userId = createUser().getId();
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
	void deleteTemplateExcludesFromList() {
		UUID userId = createUser().getId();
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
	void builtinTemplateCannotBeDeleted() {
		UUID userId = createUser().getId();
		Template builtin = Template.createBuiltin("BASIC", 1, "기본", "기본 설명");
		builtin.initializeQuestions(List.of(TemplateQuestionRequestFixture.requiredQuestion("질문")));
		Template saved = templateRepository.save(builtin);

		assertThatThrownBy(() -> templateService.deleteTemplate(userId, saved.getId()))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode")
					.isEqualTo(TemplateErrorCode.BUILTIN_TEMPLATE_DELETION_NOT_ALLOWED);
	}

	@Test
	void getTemplatesReturnsBuiltinTemplatesInSpecOrder() {
		UUID userId = createUser().getId();
		templateRepository.save(Template.createBuiltin("IMMERSION_CHALLENGE", 1, "몰입·도전", "설명"));
		templateRepository.save(Template.createBuiltin("PROBLEM_SOLVING_RESULT", 1, "문제해결·성과", "설명"));
		templateRepository.save(Template.createBuiltin("COLLABORATION_CONFLICT", 1, "협업·갈등", "설명"));
		templateRepository.save(Template.createBuiltin("IDEA_PLANNING", 1, "아이디어·기획", "설명"));

		assertThat(templateService.getTemplates(userId).stream()
				.filter(TemplateResponse::isBuiltin)
				.map(TemplateResponse::title))
				.containsExactly("아이디어·기획", "협업·갈등", "문제해결·성과", "몰입·도전");
		assertThat(BuiltinTemplateInitializer.DEFAULT_TEMPLATE_CODES).hasSize(4);
	}

	private static class TemplateQuestionRequestFixture {

		private static TemplateQuestion requiredQuestion(String text) {
			return TemplateQuestion.create(text, null, true, 1);
		}
	}

	private User createUser() {
		return userRepository.save(User.of(
				UUID.randomUUID() + "@test.com",
				"encoded-password",
				"테스터"
		));
	}
}
