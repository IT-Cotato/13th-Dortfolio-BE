package com.itcotato.dortfolio.domain.template.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.itcotato.dortfolio.domain.template.entity.Template;
import com.itcotato.dortfolio.domain.template.entity.TemplateQuestion;
import com.itcotato.dortfolio.domain.template.repository.ActivityTemplateRepository;
import com.itcotato.dortfolio.domain.template.repository.TemplateRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class BuiltinTemplateInitializerTest {

	@Autowired
	private TemplateRepository templateRepository;

	@Autowired
	private ActivityTemplateRepository activityTemplateRepository;

	@Autowired
	private ApplicationRunner initializeBuiltinTemplates;

	@BeforeEach
	void setUp() {
		activityTemplateRepository.deleteAll();
		templateRepository.deleteAll();
	}

	@Test
	void initializeBuiltinTemplatesUpsertsByBuiltinCode() throws Exception {
		Template oldTemplate = Template.createBuiltin("IDEA_PLANNING", 0, "이전 제목", "이전 설명");
		oldTemplate.addQuestion(TemplateQuestion.createBuiltin(
			"IDEA_PLANNING_BACKGROUND",
			"이전 질문",
			null,
			true,
			1
		));
		oldTemplate.addQuestion(TemplateQuestion.createBuiltin(
			"IDEA_PLANNING_REMOVED",
			"삭제된 질문",
			null,
			false,
			2
		));
		templateRepository.save(oldTemplate);

		initializeBuiltinTemplates.run(new DefaultApplicationArguments());

		Template template = templateRepository.findByBuiltinCode("IDEA_PLANNING").orElseThrow();
		UUID firstQuestionId = template.getQuestions().stream()
			.filter(question -> "IDEA_PLANNING_BACKGROUND".equals(question.getBuiltinCode()))
			.findFirst()
			.orElseThrow()
			.getId();

		initializeBuiltinTemplates.run(new DefaultApplicationArguments());

		Template reloadedTemplate = templateRepository.findByBuiltinCode("IDEA_PLANNING").orElseThrow();
		UUID reloadedQuestionId = reloadedTemplate.getQuestions().stream()
			.filter(question -> "IDEA_PLANNING_BACKGROUND".equals(question.getBuiltinCode()))
			.findFirst()
			.orElseThrow()
			.getId();

		assertThat(template.getTitle()).isEqualTo("아이디어·기획");
		assertThat(template.getDescription()).isEqualTo("아이디어와 기획 과정을 정리하는 템플릿");
		assertThat(template.getBuiltinVersion()).isEqualTo(1);
		assertThat(template.getQuestions()).filteredOn(question -> !question.isDeleted()).hasSize(3);
		assertThat(template.getQuestions())
			.filteredOn(question -> "IDEA_PLANNING_REMOVED".equals(question.getBuiltinCode()))
			.singleElement()
			.extracting(TemplateQuestion::isDeleted)
			.isEqualTo(true);
		assertThat(firstQuestionId).isEqualTo(reloadedQuestionId);
		assertThat(templateRepository.findAll().stream().filter(Template::isBuiltin)).hasSize(4);
		assertThat(templateRepository.findAll().stream()
			.filter(Template::isBuiltin)
			.map(Template::getTitle))
			.containsExactlyInAnyOrder("아이디어·기획", "협업·갈등", "문제해결·성과", "몰입·도전");
	}

}
