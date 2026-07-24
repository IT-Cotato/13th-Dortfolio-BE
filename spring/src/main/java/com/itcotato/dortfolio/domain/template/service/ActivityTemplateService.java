package com.itcotato.dortfolio.domain.template.service;

import com.itcotato.dortfolio.domain.activity.entity.Activity;
import com.itcotato.dortfolio.domain.activity.repository.ActivityRepository;
import com.itcotato.dortfolio.domain.template.dto.req.ActivityTemplateUpdateRequest;
import com.itcotato.dortfolio.domain.template.dto.res.ActivityTemplateResponse;
import com.itcotato.dortfolio.domain.template.entity.ActivityTemplate;
import com.itcotato.dortfolio.domain.template.entity.Template;
import com.itcotato.dortfolio.domain.template.repository.ActivityTemplateRepository;
import com.itcotato.dortfolio.domain.template.repository.TemplateRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import com.itcotato.dortfolio.global.exception.types.GlobalErrorCode;
import com.itcotato.dortfolio.global.exception.types.TemplateErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityTemplateService {

	private static final int MAX_TEMPLATE_SELECTION_COUNT = 4;

	private final ActivityTemplateRepository activityTemplateRepository;
	private final TemplateRepository templateRepository;
	private final ActivityRepository activityRepository;

	@Transactional(readOnly = true)
	public List<ActivityTemplateResponse> getActivityTemplates(UUID userId, UUID activityId) {
		validateActivityAccess(userId, activityId);

		List<ActivityTemplate> activityTemplates = activityTemplateRepository.findAllByActivity_IdOrderBySortOrderAsc(activityId);
		List<UUID> templateIds = activityTemplates.stream()
			.map(ActivityTemplate::getTemplateId)
			.toList();

		Map<UUID, Template> templates = templateRepository.findAllByIdInAndDeletedAtIsNull(templateIds).stream()
			.collect(java.util.stream.Collectors.toMap(Template::getId, Function.identity()));
		templates.values().forEach(template -> validateReadable(template, userId));

		return activityTemplates.stream()
			.filter(activityTemplate -> templates.containsKey(activityTemplate.getTemplateId()))
			.map(activityTemplate -> ActivityTemplateResponse.of(
				templates.get(activityTemplate.getTemplateId()),
				activityTemplate.getSortOrder()
			))
			.toList();
	}

	@Transactional
	public List<ActivityTemplateResponse> updateActivityTemplates(
		UUID userId,
		UUID activityId,
		ActivityTemplateUpdateRequest request
	) {
		validateSelection(request.templateIds());
		Activity activity = validateActivityAccess(userId, activityId);
		List<Template> templates = getReadableTemplates(userId, request.templateIds());
		List<ActivityTemplate> activityTemplates = replaceActivityTemplates(activity, templates);

		return toResponses(activityTemplates, templates);
	}

	private Activity validateActivityAccess(UUID userId, UUID activityId) {
		return activityRepository.findByIdAndUser_Id(activityId, userId)
			.orElseThrow(() -> new CustomException(GlobalErrorCode.INVALID_INPUT_VALUE));
	}

	private void validateSelection(List<UUID> templateIds) {
		if (templateIds.size() > MAX_TEMPLATE_SELECTION_COUNT) {
			throw new CustomException(TemplateErrorCode.ACTIVITY_TEMPLATE_LIMIT_EXCEEDED);
		}
		if (new HashSet<>(templateIds).size() != templateIds.size()) {
			throw new CustomException(TemplateErrorCode.DUPLICATE_TEMPLATE_SELECTION);
		}
	}

	private List<Template> getReadableTemplates(UUID userId, List<UUID> templateIds) {
		Map<UUID, Template> templates = templateRepository.findAllByIdInAndDeletedAtIsNull(templateIds).stream()
			.collect(java.util.stream.Collectors.toMap(Template::getId, Function.identity()));
		return templateIds.stream()
			.map(templateId -> {
				Template template = templates.get(templateId);
				if (template == null) {
					throw new CustomException(TemplateErrorCode.TEMPLATE_NOT_FOUND);
				}
				validateReadable(template, userId);
				return template;
			})
			.toList();
	}

	private List<ActivityTemplate> replaceActivityTemplates(Activity activity, List<Template> templates) {
		UUID activityId = activity.getId();
		List<UUID> templateIds = templates.stream()
			.map(Template::getId)
			.toList();
		List<ActivityTemplate> existingActivityTemplates =
			activityTemplateRepository.findAllByActivity_IdOrderBySortOrderAsc(activityId);
		Map<UUID, ActivityTemplate> existingActivityTemplatesByTemplateId = existingActivityTemplates.stream()
			.collect(java.util.stream.Collectors.toMap(ActivityTemplate::getTemplateId, Function.identity()));
		Set<UUID> selectedTemplateIds = new HashSet<>(templateIds);

		List<ActivityTemplate> removedActivityTemplates = existingActivityTemplates.stream()
			.filter(activityTemplate -> !selectedTemplateIds.contains(activityTemplate.getTemplateId()))
			.toList();
		activityTemplateRepository.deleteAll(removedActivityTemplates);

		List<ActivityTemplate> activityTemplates = new java.util.ArrayList<>();
		for (int index = 0; index < templateIds.size(); index++) {
			UUID templateId = templateIds.get(index);
			ActivityTemplate activityTemplate = existingActivityTemplatesByTemplateId.get(templateId);
			if (activityTemplate == null) {
				activityTemplate = ActivityTemplate.create(activity, templates.get(index), index + 1);
			} else {
				activityTemplate.updateSortOrder(index + 1);
			}
			activityTemplates.add(activityTemplate);
		}

		return activityTemplateRepository.saveAll(activityTemplates);
	}

	private List<ActivityTemplateResponse> toResponses(List<ActivityTemplate> activityTemplates, List<Template> templates) {
		Map<UUID, Template> templatesById = templates.stream()
			.collect(java.util.stream.Collectors.toMap(Template::getId, Function.identity()));

		return activityTemplates.stream()
			.map(activityTemplate -> ActivityTemplateResponse.of(
				templatesById.get(activityTemplate.getTemplateId()),
				activityTemplate.getSortOrder()
			))
			.toList();
	}

	private void validateReadable(Template template, UUID userId) {
		if (template.isBuiltin() || userId.equals(template.getUserId())) {
			return;
		}
		throw new CustomException(TemplateErrorCode.TEMPLATE_FORBIDDEN);
	}
}
