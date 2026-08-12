package com.itcotato.dortfolio.domain.record.service;

import com.itcotato.dortfolio.domain.activity.entity.Activity;
import com.itcotato.dortfolio.domain.activity.repository.ActivityRepository;
import com.itcotato.dortfolio.domain.template.entity.Template;
import com.itcotato.dortfolio.domain.template.repository.TemplateRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.RecordErrorCode;
import com.itcotato.dortfolio.global.exception.types.TemplateErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecordValidator {

	private final ActivityRepository activityRepository;
	private final TemplateRepository templateRepository;

	public Activity getActiveActivityOrThrow(UUID userId, UUID activityId) {
		Activity activity = activityRepository.findByIdAndUser_Id(activityId, userId)
			.orElseThrow(() -> new CustomException(RecordErrorCode.RECORD_ACTIVITY_NOT_FOUND));

		if (activity.isDeleted()) {
			throw new CustomException(RecordErrorCode.RECORD_ACTIVITY_NOT_FOUND);
		}

		return activity;
	}

	public Template getReadableActiveTemplateOrThrow(UUID userId, UUID templateId) {
		Template template = templateRepository.findByIdAndDeletedAtIsNull(templateId)
			.orElseThrow(() -> new CustomException(TemplateErrorCode.TEMPLATE_NOT_FOUND));
		validateReadable(template, userId);
		return template;
	}

	public Template getReadableTemplateOrThrow(UUID userId, UUID templateId) {
		Template template = templateRepository.findById(templateId)
			.orElseThrow(() -> new CustomException(TemplateErrorCode.TEMPLATE_NOT_FOUND));
		validateReadable(template, userId);
		return template;
	}

	private void validateReadable(Template template, UUID userId) {
		if (template.isBuiltin() || userId.equals(template.getUserId())) {
			return;
		}

		throw new CustomException(TemplateErrorCode.TEMPLATE_FORBIDDEN);
	}

}
