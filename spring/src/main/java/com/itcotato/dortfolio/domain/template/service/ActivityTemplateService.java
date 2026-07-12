package com.itcotato.dortfolio.domain.template.service;

import com.itcotato.dortfolio.domain.template.dto.ActivityTemplateUpdateRequest;
import com.itcotato.dortfolio.domain.template.dto.ActivityTemplateResponse;
import com.itcotato.dortfolio.domain.template.dto.TemplateResponse;
import com.itcotato.dortfolio.domain.template.entity.ActivityTemplate;
import com.itcotato.dortfolio.domain.template.entity.Template;
import com.itcotato.dortfolio.domain.template.repository.ActivityTemplateRepository;
import com.itcotato.dortfolio.domain.template.repository.TemplateRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.ErrorCode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityTemplateService {

	private static final int MAX_TEMPLATE_SELECTION_COUNT = 4;

	private final ActivityTemplateRepository activityTemplateRepository;
	private final TemplateRepository templateRepository;

	@Transactional(readOnly = true)
	public List<ActivityTemplateResponse> getActivityTemplates(UUID userId, UUID activityId) {
		validateActivityAccess(userId, activityId);

		List<ActivityTemplate> activityTemplates = activityTemplateRepository.findAllByActivityIdOrderBySortOrderAsc(activityId);
		List<UUID> templateIds = activityTemplates.stream()
			.map(ActivityTemplate::getTemplateId)
			.toList();

		Map<UUID, Template> templates = templateRepository.findAllById(templateIds).stream()
			.filter(template -> !template.isDeleted())
			.peek(template -> validateReadable(template, userId))
			.collect(java.util.stream.Collectors.toMap(Template::getId, Function.identity()));

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
		validateActivityAccess(userId, activityId);
		validateSelection(request.templateIds());
		List<Template> templates = request.templateIds().stream()
			.map(templateId -> getReadableTemplate(userId, templateId))
			.toList();

		activityTemplateRepository.deleteAllByActivityIdInBulk(activityId);
		activityTemplateRepository.flush();
		List<ActivityTemplate> activityTemplates = IntStream.range(0, request.templateIds().size())
			.mapToObj(index -> ActivityTemplate.create(activityId, request.templateIds().get(index), index + 1))
			.toList();
		activityTemplateRepository.saveAll(activityTemplates);

		return IntStream.range(0, templates.size())
			.mapToObj(index -> ActivityTemplateResponse.of(templates.get(index), index + 1))
			.toList();
	}

	private void validateActivityAccess(UUID userId, UUID activityId) {
		// TODO: Activity 도메인 병합 후 activityId 존재 여부와 userId 소유 여부를 함께 검증한다.
	}

	private void validateSelection(List<UUID> templateIds) {
		if (templateIds.size() > MAX_TEMPLATE_SELECTION_COUNT) {
			throw new CustomException(ErrorCode.ACTIVITY_TEMPLATE_LIMIT_EXCEEDED);
		}
		if (new HashSet<>(templateIds).size() != templateIds.size()) {
			throw new CustomException(ErrorCode.DUPLICATE_TEMPLATE_SELECTION);
		}
	}

	private Template getReadableTemplate(UUID userId, UUID templateId) {
		Template template = templateRepository.findByIdAndDeletedAtIsNull(templateId)
			.orElseThrow(() -> new CustomException(ErrorCode.TEMPLATE_NOT_FOUND));
		validateReadable(template, userId);
		return template;
	}

	private void validateReadable(Template template, UUID userId) {
		if (template.isBuiltin() || userId.equals(template.getUserId())) {
			return;
		}
		throw new CustomException(ErrorCode.TEMPLATE_FORBIDDEN);
	}
}
