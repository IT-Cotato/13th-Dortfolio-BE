package com.itcotato.dortfolio.domain.activity.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.itcotato.dortfolio.domain.activity.dto.req.ActivityCreateRequest;
import com.itcotato.dortfolio.domain.activity.entity.ActivityType;
import com.itcotato.dortfolio.domain.activity.repository.ActivityRepository;
import com.itcotato.dortfolio.domain.activity.repository.ActivityTypeRepository;
import com.itcotato.dortfolio.domain.record.entity.Record;
import com.itcotato.dortfolio.domain.record.repository.RecordRepository;
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
import org.junit.jupiter.api.DisplayName;
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
	private RecordRepository recordRepository;

	@Autowired
	private ApplicationRunner initializeBuiltinTemplates;

	@BeforeEach
	void setUp() throws Exception {
		recordRepository.deleteAll();
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

	@Test
	@DisplayName("활동을 삭제하면 그 안의 기록도 함께 삭제된다")
	void deleteActivityAlsoDeletesRecords() {
		User user = createUser();
		UUID activityId = createActivity(user);
		Record record = createRecord(user, activityId);

		activityService.deleteActivity(user.getId(), activityId);

		assertThat(activityRepository.findById(activityId).orElseThrow().isDeleted()).isTrue();
		assertThat(recordRepository.findById(record.getId()).orElseThrow().isDeleted()).isTrue();
	}

	@Test
	@DisplayName("활동을 복구하면 함께 삭제됐던 기록도 되살아난다")
	void restoreActivityAlsoRestoresRecords() {
		User user = createUser();
		UUID activityId = createActivity(user);
		Record record = createRecord(user, activityId);

		activityService.deleteActivity(user.getId(), activityId);
		activityService.restoreActivity(user.getId(), activityId);

		assertThat(activityRepository.findById(activityId).orElseThrow().isDeleted()).isFalse();
		assertThat(recordRepository.findById(record.getId()).orElseThrow().isDeleted()).isFalse();
	}

	@Test
	@DisplayName("활동 삭제 전에 개별 삭제한 기록은 복구되지 않는다")
	void restoreActivityDoesNotReviveSeparatelyDeletedRecords() throws InterruptedException {
		User user = createUser();
		UUID activityId = createActivity(user);
		Record deletedEarlier = createRecord(user, activityId);
		Record deletedWithActivity = createRecord(user, activityId);

		// 사용자가 먼저 기록 하나만 지운 상황
		deletedEarlier.markDeleted(1);
		recordRepository.saveAndFlush(deletedEarlier);

		// 삭제 시각으로 구분하므로 두 삭제의 시각이 확실히 달라야 한다
		Thread.sleep(10);

		activityService.deleteActivity(user.getId(), activityId);
		activityService.restoreActivity(user.getId(), activityId);

		assertThat(recordRepository.findById(deletedEarlier.getId()).orElseThrow().isDeleted()).isTrue();
		assertThat(recordRepository.findById(deletedWithActivity.getId()).orElseThrow().isDeleted()).isFalse();
	}

	private User createUser() {
		return userRepository.save(User.of(
				UUID.randomUUID() + "@test.com",
				"encoded-password",
				"테스터"
		));
	}

	private UUID createActivity(User user) {
		ActivityType activityType = activityTypeRepository.save(ActivityType.create(user, "프로젝트"));
		return activityService.createActivity(user.getId(), new ActivityCreateRequest(
				activityType.getId(), "도트폴리오", "설명", LocalDate.now(), null, true));
	}

	private Record createRecord(User user, UUID activityId) {
		Template template = templateRepository
				.findAllByBuiltinCodeInAndDeletedAtIsNull(BuiltinTemplateInitializer.DEFAULT_TEMPLATE_CODES)
				.get(0);

		return recordRepository.save(Record.builder()
				.user(user)
				.activity(activityRepository.findById(activityId).orElseThrow())
				.template(template)
				.title("기록")
				.build());
	}
}
