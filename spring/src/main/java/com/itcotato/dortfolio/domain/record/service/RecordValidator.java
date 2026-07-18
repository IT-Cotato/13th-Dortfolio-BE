package com.itcotato.dortfolio.domain.record.service;

import com.itcotato.dortfolio.activity.entity.Activity;
import com.itcotato.dortfolio.activity.repository.ActivityRepository;
import com.itcotato.dortfolio.domain.template.entity.Template;
import com.itcotato.dortfolio.domain.template.repository.ActivityTemplateRepository;
import com.itcotato.dortfolio.domain.template.repository.TemplateRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.ErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecordValidator {

	private final ActivityRepository activityRepository;
	private final TemplateRepository templateRepository;
	private final ActivityTemplateRepository activityTemplateRepository;

	public Activity getActiveActivityOrThrow(UUID userId, UUID activityId) {
		Activity activity = activityRepository.findByIdAndUser_Id(activityId, userId)
			.orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE));

		if (activity.isDeleted()) {
			throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
		}

		return activity;
	}

	public Template getReadableActiveTemplateOrThrow(UUID userId, UUID templateId) {
		Template template = templateRepository.findByIdAndDeletedAtIsNull(templateId)
			.orElseThrow(() -> new CustomException(ErrorCode.TEMPLATE_NOT_FOUND));

		if (template.isBuiltin() || userId.equals(template.getUserId())) {
			return template;
		}

		throw new CustomException(ErrorCode.TEMPLATE_FORBIDDEN);
	}

	public void validateActivityTemplate(UUID activityId, UUID templateId) {
		if (!activityTemplateRepository.existsByActivity_IdAndTemplate_Id(activityId, templateId)) {
			throw new CustomException(ErrorCode.RECORD_TEMPLATE_NOT_CONNECTED);
		}
	}
}
