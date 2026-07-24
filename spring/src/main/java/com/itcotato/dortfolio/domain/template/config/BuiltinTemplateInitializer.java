package com.itcotato.dortfolio.domain.template.config;

import com.itcotato.dortfolio.domain.template.entity.Template;
import com.itcotato.dortfolio.domain.template.entity.TemplateQuestion;
import com.itcotato.dortfolio.domain.template.repository.TemplateRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class BuiltinTemplateInitializer {

	public static final List<String> DEFAULT_TEMPLATE_CODES = List.of(
		"IDEA_PLANNING",
		"COLLABORATION_CONFLICT",
		"PROBLEM_SOLVING_RESULT",
		"IMMERSION_CHALLENGE"
	);

	private final TemplateRepository templateRepository;

	@Bean
	ApplicationRunner initializeBuiltinTemplates() {
		// TODO: Flyway 도입 후 기본 템플릿 seed를 DB migration으로 이관하고 이 initializer를 제거한다.
		return args -> defaultTemplates().stream()
			.map(this::upsert)
			.forEach(templateRepository::save);
	}

	private Template upsert(DefaultTemplate defaultTemplate) {
		return templateRepository.findByBuiltinCode(defaultTemplate.code())
			.map(defaultTemplate::applyTo)
			.orElseGet(defaultTemplate::toEntity);
	}

	private List<DefaultTemplate> defaultTemplates() {
		return List.of(
				new DefaultTemplate(
					"IDEA_PLANNING",
					1,
					"아이디어·기획",
					"아이디어와 기획 과정을 정리하는 템플릿",
					List.of(
						new DefaultQuestion("IDEA_PLANNING_BACKGROUND", "배경", "아이디어가 나온 배경을 적어주세요.", true),
						new DefaultQuestion("IDEA_PLANNING_IDEA", "아이디어", "기획한 아이디어를 적어주세요.", true),
						new DefaultQuestion("IDEA_PLANNING_PLAN", "실행 계획", "구체적인 실행 계획을 적어주세요.", false)
					)
				),
				new DefaultTemplate(
					"COLLABORATION_CONFLICT",
					1,
					"협업·갈등",
					"협업과 갈등 해결 경험을 정리하는 템플릿",
					List.of(
						new DefaultQuestion("COLLABORATION_CONFLICT_SITUATION", "상황", "협업 상황을 적어주세요.", true),
						new DefaultQuestion("COLLABORATION_CONFLICT_ACTION", "대응", "갈등이나 협업 이슈에 어떻게 대응했는지 적어주세요.", true),
						new DefaultQuestion("COLLABORATION_CONFLICT_LESSON", "배운 점", "협업을 통해 배운 점을 적어주세요.", false)
					)
				),
				new DefaultTemplate(
					"PROBLEM_SOLVING_RESULT",
					1,
					"문제해결·성과",
					"문제 해결 과정과 성과를 정리하는 템플릿",
					List.of(
						new DefaultQuestion("PROBLEM_SOLVING_RESULT_PROBLEM", "문제", "해결해야 했던 문제를 적어주세요.", true),
						new DefaultQuestion("PROBLEM_SOLVING_RESULT_SOLUTION", "해결 과정", "문제를 해결한 과정을 적어주세요.", true),
						new DefaultQuestion("PROBLEM_SOLVING_RESULT_OUTCOME", "성과", "결과와 성과를 적어주세요.", true)
					)
				),
				new DefaultTemplate(
					"IMMERSION_CHALLENGE",
					1,
					"몰입·도전",
					"몰입과 도전 경험을 정리하는 템플릿",
					List.of(
						new DefaultQuestion("IMMERSION_CHALLENGE_GOAL", "목표", "도전한 목표를 적어주세요.", true),
						new DefaultQuestion("IMMERSION_CHALLENGE_EFFORT", "몰입 과정", "몰입해서 노력한 과정을 적어주세요.", true),
						new DefaultQuestion("IMMERSION_CHALLENGE_GROWTH", "성장", "도전 후 성장한 점을 적어주세요.", false)
					)
				)
			);
	}

	private record DefaultTemplate(
		String code,
		int version,
		String title,
		String description,
		List<DefaultQuestion> questions
	) {

		private Template toEntity() {
			Template template = Template.createBuiltin(code, version, title, description);
			upsertQuestions(template);
			return template;
		}

		private Template applyTo(Template template) {
			template.updateBuiltin(version, title, description);
			upsertQuestions(template);
			return template;
		}

		private void upsertQuestions(Template template) {
			List<TemplateQuestion> templateQuestions = new java.util.ArrayList<>();
			for (int i = 0; i < questions.size(); i++) {
				DefaultQuestion question = questions.get(i);
				templateQuestions.add(TemplateQuestion.createBuiltin(
					question.code(),
					question.questionText(),
					question.description(),
					question.required(),
					i + 1
				));
			}
			template.upsertBuiltinQuestions(templateQuestions);
		}
	}

	private record DefaultQuestion(String code, String questionText, String description, boolean required) {
	}
}
