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
		return args -> defaultTemplates().stream()
			.filter(template -> !templateRepository.existsByTitleAndIsBuiltinTrue(template.title()))
			.map(DefaultTemplate::toEntity)
			.forEach(templateRepository::save);
	}

	private List<DefaultTemplate> defaultTemplates() {
		return List.of(
			new DefaultTemplate(
				"프로젝트 경험",
				"프로젝트 활동을 정리하는 템플릿",
				List.of(
					new DefaultQuestion("진행한 일", "맡은 역할과 작업을 적어주세요.", true),
					new DefaultQuestion("문제 상황", "어려웠던 점을 적어주세요.", false),
					new DefaultQuestion("해결 방법", "어떻게 해결했는지 적어주세요.", true)
				)
			),
			new DefaultTemplate(
				"문제 해결",
				"문제와 해결 과정을 정리하는 템플릿",
				List.of(
					new DefaultQuestion("문제 정의", "해결해야 했던 문제를 적어주세요.", true),
					new DefaultQuestion("시도한 방법", "시도한 접근을 적어주세요.", true),
					new DefaultQuestion("결과", "결과와 배운 점을 적어주세요.", true)
				)
			),
			new DefaultTemplate(
				"협업 경험",
				"팀 활동과 소통 경험을 정리하는 템플릿",
				List.of(
					new DefaultQuestion("상황", "협업 상황을 적어주세요.", true),
					new DefaultQuestion("기여", "내가 기여한 부분을 적어주세요.", true),
					new DefaultQuestion("배운 점", "협업을 통해 배운 점을 적어주세요.", false)
				)
			),
			new DefaultTemplate(
				"회고",
				"활동 후 회고를 남기는 템플릿",
				List.of(
					new DefaultQuestion("잘한 점", "잘했다고 생각한 점을 적어주세요.", true),
					new DefaultQuestion("아쉬운 점", "개선하고 싶은 점을 적어주세요.", true),
					new DefaultQuestion("다음 목표", "다음에 시도할 일을 적어주세요.", false)
				)
			)
		);
	}

	private record DefaultTemplate(String title, String description, List<DefaultQuestion> questions) {

		private Template toEntity() {
			Template template = Template.createBuiltin(title, description);
			for (int i = 0; i < questions.size(); i++) {
				DefaultQuestion question = questions.get(i);
				template.addQuestion(TemplateQuestion.create(
					question.questionText(),
					question.description(),
					question.required(),
					i + 1
				));
			}
			return template;
		}
	}

	private record DefaultQuestion(String questionText, String description, boolean required) {
	}
}
