package com.itcotato.dortfolio.domain.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.itcotato.dortfolio.domain.template.dto.ActivityTemplateResponse;
import com.itcotato.dortfolio.domain.template.dto.ActivityTemplateUpdateRequest;
import com.itcotato.dortfolio.domain.template.dto.TemplateCreateRequest;
import com.itcotato.dortfolio.domain.template.dto.TemplateQuestionRequest;
import com.itcotato.dortfolio.domain.template.dto.TemplateResponse;
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
class ActivityTemplateServiceTest {

	@Autowired
	private ActivityTemplateService activityTemplateService;

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
	void updateActivityTemplates() {
		UUID userId = UUID.randomUUID();
		UUID activityId = UUID.randomUUID();
		TemplateResponse first = createTemplate(userId, "템플릿1");
		TemplateResponse second = createTemplate(userId, "템플릿2");

		List<ActivityTemplateResponse> responses = activityTemplateService.updateActivityTemplates(
			userId,
			activityId,
			new ActivityTemplateUpdateRequest(List.of(first.id(), second.id()))
		);

		assertThat(responses).extracting(ActivityTemplateResponse::id).containsExactly(first.id(), second.id());
		assertThat(activityTemplateService.getActivityTemplates(userId, activityId))
			.extracting(ActivityTemplateResponse::id)
			.containsExactly(first.id(), second.id());
	}

	@Test
	void updateActivityTemplatesRejectsMoreThanFourTemplates() {
		UUID userId = UUID.randomUUID();
		UUID activityId = UUID.randomUUID();
		List<UUID> templateIds = List.of(
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID()
		);

		assertThatThrownBy(() -> activityTemplateService.updateActivityTemplates(
			userId,
			activityId,
			new ActivityTemplateUpdateRequest(templateIds)
		))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.ACTIVITY_TEMPLATE_LIMIT_EXCEEDED);
	}

	@Test
	void updateActivityTemplatesRejectsDuplicatedTemplate() {
		UUID userId = UUID.randomUUID();
		UUID activityId = UUID.randomUUID();
		TemplateResponse template = createTemplate(userId, "템플릿");

		assertThatThrownBy(() -> activityTemplateService.updateActivityTemplates(
			userId,
			activityId,
			new ActivityTemplateUpdateRequest(List.of(template.id(), template.id()))
		))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.DUPLICATE_TEMPLATE_SELECTION);
	}

	private TemplateResponse createTemplate(UUID userId, String title) {
		return templateService.createTemplate(userId, new TemplateCreateRequest(
			title,
			null,
			List.of(new TemplateQuestionRequest("질문", null, true))
		));
	}
}
