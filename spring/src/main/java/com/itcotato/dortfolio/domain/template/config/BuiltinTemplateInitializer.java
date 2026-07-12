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
				"PROJECT_EXPERIENCE",
				1,
				"프로젝트 경험",
				"프로젝트 활동을 정리하는 템플릿",
				List.of(
					new DefaultQuestion("PROJECT_EXPERIENCE_WORK", "진행한 일", "맡은 역할과 작업을 적어주세요.", true),
					new DefaultQuestion("PROJECT_EXPERIENCE_PROBLEM", "문제 상황", "어려웠던 점을 적어주세요.", false),
					new DefaultQuestion("PROJECT_EXPERIENCE_SOLUTION", "해결 방법", "어떻게 해결했는지 적어주세요.", true)
				)
			),
			new DefaultTemplate(
				"PROBLEM_SOLVING",
				1,
				"문제 해결",
				"문제와 해결 과정을 정리하는 템플릿",
				List.of(
					new DefaultQuestion("PROBLEM_SOLVING_DEFINITION", "문제 정의", "해결해야 했던 문제를 적어주세요.", true),
					new DefaultQuestion("PROBLEM_SOLVING_TRY", "시도한 방법", "시도한 접근을 적어주세요.", true),
					new DefaultQuestion("PROBLEM_SOLVING_RESULT", "결과", "결과와 배운 점을 적어주세요.", true)
				)
			),
			new DefaultTemplate(
				"COLLABORATION",
				1,
				"협업 경험",
				"팀 활동과 소통 경험을 정리하는 템플릿",
				List.of(
					new DefaultQuestion("COLLABORATION_SITUATION", "상황", "협업 상황을 적어주세요.", true),
					new DefaultQuestion("COLLABORATION_CONTRIBUTION", "기여", "내가 기여한 부분을 적어주세요.", true),
					new DefaultQuestion("COLLABORATION_LESSON", "배운 점", "협업을 통해 배운 점을 적어주세요.", false)
				)
			),
			new DefaultTemplate(
				"RETROSPECTIVE",
				1,
				"회고",
				"활동 후 회고를 남기는 템플릿",
				List.of(
					new DefaultQuestion("RETROSPECTIVE_GOOD", "잘한 점", "잘했다고 생각한 점을 적어주세요.", true),
					new DefaultQuestion("RETROSPECTIVE_IMPROVE", "아쉬운 점", "개선하고 싶은 점을 적어주세요.", true),
					new DefaultQuestion("RETROSPECTIVE_NEXT", "다음 목표", "다음에 시도할 일을 적어주세요.", false)
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
