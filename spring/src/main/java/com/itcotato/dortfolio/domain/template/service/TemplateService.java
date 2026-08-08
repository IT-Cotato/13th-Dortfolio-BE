package com.itcotato.dortfolio.domain.template.service;

import com.itcotato.dortfolio.domain.template.dto.req.TemplateCreateRequest;
import com.itcotato.dortfolio.domain.template.dto.req.TemplateQuestionRequest;
import com.itcotato.dortfolio.domain.template.dto.res.TemplateResponse;
import com.itcotato.dortfolio.domain.template.entity.Template;
import com.itcotato.dortfolio.domain.template.entity.TemplateQuestion;
import com.itcotato.dortfolio.domain.template.repository.TemplateRepository;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.TemplateErrorCode;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TemplateService {

	private final TemplateRepository templateRepository;
	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public List<TemplateResponse> getTemplates(UUID userId) {
		return templateRepository.findAvailableTemplates(userId).stream()
			.map(TemplateResponse::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public TemplateResponse getTemplate(UUID userId, UUID templateId) {
		Template template = getActiveTemplate(templateId);
		validateReadable(template, userId);
		return TemplateResponse.from(template);
	}

	@Transactional
	public TemplateResponse createTemplate(UUID userId, TemplateCreateRequest request) {
		User user = getUserOrThrow(userId);
		Template template = Template.createCustom(user, request.title(), request.description());
		template.initializeQuestions(toQuestions(request.questions()));
		return TemplateResponse.from(templateRepository.save(template));
	}

	@Transactional
	public void deleteTemplate(UUID userId, UUID templateId) {
		Template template = getActiveTemplate(templateId);
		validateDeletable(template, userId);
		template.delete();
	}

	private Template getActiveTemplate(UUID templateId) {
		return templateRepository.findByIdAndDeletedAtIsNull(templateId)
			.orElseThrow(() -> new CustomException(TemplateErrorCode.TEMPLATE_NOT_FOUND));
	}

	private void validateReadable(Template template, UUID userId) {
		if (template.isBuiltin() || userId.equals(template.getUserId())) {
			return;
		}
		throw new CustomException(TemplateErrorCode.TEMPLATE_FORBIDDEN);
	}

	private void validateDeletable(Template template, UUID userId) {
		if (template.isBuiltin()) {
			throw new CustomException(TemplateErrorCode.BUILTIN_TEMPLATE_DELETION_NOT_ALLOWED);
		}
		if (!userId.equals(template.getUserId())) {
			throw new CustomException(TemplateErrorCode.TEMPLATE_FORBIDDEN);
		}
	}

	private List<TemplateQuestion> toQuestions(List<TemplateQuestionRequest> requests) {
		return IntStream.range(0, requests.size())
			.mapToObj(index -> {
				TemplateQuestionRequest request = requests.get(index);
				return TemplateQuestion.create(
					request.questionText(),
					request.description(),
					request.required(),
					index + 1
				);
			})
			.toList();
	}

	private User getUserOrThrow(UUID userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(TemplateErrorCode.TEMPLATE_USER_NOT_FOUND));
	}
}
