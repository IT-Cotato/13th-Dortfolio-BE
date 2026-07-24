package com.itcotato.dortfolio.domain.activity.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.itcotato.dortfolio.domain.activity.dto.ActivityCreateRequest;
import com.itcotato.dortfolio.domain.activity.entity.ActivityType;
import com.itcotato.dortfolio.domain.activity.repository.ActivityRepository;
import com.itcotato.dortfolio.domain.activity.repository.ActivityTypeRepository;
import com.itcotato.dortfolio.domain.template.config.BuiltinTemplateInitializer;
import com.itcotato.dortfolio.domain.template.entity.Template;
import com.itcotato.dortfolio.domain.template.repository.ActivityTemplateRepository;
import com.itcotato.dortfolio.domain.template.repository.TemplateRepository;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class ActivityServiceTest {

	@Autowired
	private ActivityService activityService;

	@Autowired
	private ActivityTemplateRepository activityTemplateRepository;

	@Autowired
	private TemplateRepository templateRepository;

	@Autowired
	private ActivityRepository activityRepository;

	@Autowired
	private ActivityTypeRepository activityTypeRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ApplicationRunner initializeBuiltinTemplates;

	@BeforeEach
	void setUp() throws Exception {
		activityTemplateRepository.deleteAll();
		templateRepository.deleteAll();
		activityRepository.deleteAll();
		activityTypeRepository.deleteAll();
		userRepository.deleteAll();
		initializeBuiltinTemplates.run(new DefaultApplicationArguments());
	}

	@Test
	void createActivityConnectsDefaultBuiltinTemplates() {
		User user = createUser();
		ActivityType activityType = activityTypeRepository.save(ActivityType.create(user, "프로젝트"));

		UUID activityId = activityService.createActivity(user.getId(), new ActivityCreateRequest(
				activityType.getId(),
				"도트폴리오",
				"설명",
				LocalDate.now(),
				null,
				true
		));
		Map<String, Template> templatesByCode = templateRepository
				.findAllByBuiltinCodeInAndDeletedAtIsNull(BuiltinTemplateInitializer.DEFAULT_TEMPLATE_CODES)
				.stream()
				.collect(java.util.stream.Collectors.toMap(Template::getBuiltinCode, Function.identity()));
		List<UUID> expectedTemplateIds = BuiltinTemplateInitializer.DEFAULT_TEMPLATE_CODES.stream()
				.map(code -> templatesByCode.get(code).getId())
				.toList();

		assertThat(activityTemplateRepository.findAllByActivity_IdOrderBySortOrderAsc(activityId))
				.extracting(activityTemplate -> activityTemplate.getTemplateId())
				.containsExactlyElementsOf(expectedTemplateIds);
	}

	private User createUser() {
		return userRepository.save(User.of(
				UUID.randomUUID() + "@test.com",
				"encoded-password",
				"테스터"
		));
	}
}
