package com.itcotato.dortfolio.domain.story.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.itcotato.dortfolio.domain.activity.entity.Activity;
import com.itcotato.dortfolio.domain.activity.entity.ActivityType;
import com.itcotato.dortfolio.domain.activity.repository.ActivityRepository;
import com.itcotato.dortfolio.domain.activity.repository.ActivityTypeRepository;
import com.itcotato.dortfolio.domain.record.analysis.repository.RecordAnalysisRepository;
import com.itcotato.dortfolio.domain.record.entity.Record;
import com.itcotato.dortfolio.domain.record.repository.CompetencyTagRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordAnswerRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordCompetencyTagRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordMemoRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordRepository;
import com.itcotato.dortfolio.domain.story.dto.res.TimelineActivityResponse;
import com.itcotato.dortfolio.domain.story.dto.res.TimelineRecordResponse;
import com.itcotato.dortfolio.domain.template.entity.Template;
import com.itcotato.dortfolio.domain.template.repository.ActivityTemplateRepository;
import com.itcotato.dortfolio.domain.template.repository.TemplateRepository;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@ActiveProfiles("test")
@SpringBootTest
class StoryServiceTest {

	@Autowired
	private StoryService storyService;

	@Autowired
	private ActivityRepository activityRepository;

	@Autowired
	private ActivityTypeRepository activityTypeRepository;

	@Autowired
	private RecordRepository recordRepository;

	@Autowired
	private TemplateRepository templateRepository;

	@Autowired
	private ActivityTemplateRepository activityTemplateRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RecordAnalysisRepository recordAnalysisRepository;

	@Autowired
	private RecordCompetencyTagRepository recordCompetencyTagRepository;

	@Autowired
	private CompetencyTagRepository competencyTagRepository;

	@Autowired
	private RecordMemoRepository recordMemoRepository;

	@Autowired
	private RecordAnswerRepository recordAnswerRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@BeforeEach
	void setUp() {
		recordAnalysisRepository.deleteAll();
		jdbcTemplate.update("delete from record_embeddings");
		recordCompetencyTagRepository.deleteAll();
		competencyTagRepository.deleteAll();
		recordMemoRepository.deleteAll();
		recordAnswerRepository.deleteAll();
		recordRepository.deleteAll();
		activityTemplateRepository.deleteAll();
		templateRepository.deleteAll();
		activityRepository.deleteAll();
		activityTypeRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	@DisplayName("보관된 활동만 타임라인에 조회된다")
	void onlyArchivedActivitiesAreReturned() {
		User user = createUser();
		createActivity(user, "진행 중 활동", LocalDate.now().minusDays(5), false);
		createActivity(user, "보관된 활동", LocalDate.now().minusDays(3), true);

		List<TimelineActivityResponse> timeline = storyService.getTimeline(user.getId());

		assertThat(timeline)
				.extracting(TimelineActivityResponse::title)
				.containsExactly("보관된 활동");
	}

	@Test
	@DisplayName("활동은 시작일 오름차순으로 정렬된다")
	void activitiesAreSortedByStartedAtAscending() {
		User user = createUser();
		createActivity(user, "나중 활동", LocalDate.now().minusDays(2), true);
		createActivity(user, "먼저 활동", LocalDate.now().minusDays(10), true);

		List<TimelineActivityResponse> timeline = storyService.getTimeline(user.getId());

		assertThat(timeline)
				.extracting(TimelineActivityResponse::title)
				.containsExactly("먼저 활동", "나중 활동");
	}

	@Test
	@DisplayName("활동 표시 항목(제목, 기간, 활동 종류)이 반환된다")
	void activityDisplayFieldsAreReturned() {
		User user = createUser();
		Activity activity = createActivity(user, "동아리 활동", LocalDate.now().minusDays(7), true);

		TimelineActivityResponse response = storyService.getTimeline(user.getId()).get(0);

		assertThat(response.activityId()).isEqualTo(activity.getId());
		assertThat(response.title()).isEqualTo("동아리 활동");
		assertThat(response.activityTypeName()).isEqualTo("동아리");
		assertThat(response.startedAt()).isEqualTo(activity.getStartedAt());
		assertThat(response.endedAt()).isEqualTo(activity.getEndedAt());
	}

	@Test
	@DisplayName("활동에 속한 기록이 작성순(오래된 순)으로 함께 반환된다")
	void recordsAreReturnedInCreatedOrder() {
		User user = createUser();
		Activity activity = createActivity(user, "활동", LocalDate.now().minusDays(5), true);
		Template template = createTemplate(user);

		// 나중에 만든 기록의 작성일을 더 과거로 바꿔서 저장 순서가 아닌 작성일 기준 정렬을 검증한다
		Record later = createRecord(user, activity, template, "나중에 작성");
		Record earlier = createRecord(user, activity, template, "먼저 작성");
		setCreatedAt(later.getId(), LocalDateTime.now().minusDays(1));
		setCreatedAt(earlier.getId(), LocalDateTime.now().minusDays(2));

		TimelineActivityResponse response = storyService.getTimeline(user.getId()).get(0);

		assertThat(response.recordCount()).isEqualTo(2);
		assertThat(response.records())
				.extracting(TimelineRecordResponse::title)
				.containsExactly("먼저 작성", "나중에 작성");
	}

	@Test
	@DisplayName("기록이 없는 활동도 빈 목록과 함께 반환된다")
	void activityWithoutRecordsIsStillReturned() {
		User user = createUser();
		createActivity(user, "기록 없는 활동", LocalDate.now().minusDays(5), true);

		TimelineActivityResponse response = storyService.getTimeline(user.getId()).get(0);

		assertThat(response.records()).isEmpty();
		assertThat(response.recordCount()).isZero();
	}

	@Test
	@DisplayName("삭제된 활동과 삭제된 기록은 타임라인에서 제외된다")
	void deletedActivityAndRecordAreExcluded() {
		User user = createUser();
		Activity deletedActivity = createActivity(user, "삭제된 활동", LocalDate.now().minusDays(9), true);
		deletedActivity.markDeleted(30);
		activityRepository.save(deletedActivity);

		Activity activity = createActivity(user, "남은 활동", LocalDate.now().minusDays(5), true);
		Template template = createTemplate(user);
		Record deletedRecord = createRecord(user, activity, template, "삭제된 기록");
		deletedRecord.markDeleted(30);
		recordRepository.save(deletedRecord);
		createRecord(user, activity, template, "남은 기록");

		List<TimelineActivityResponse> timeline = storyService.getTimeline(user.getId());

		assertThat(timeline)
				.extracting(TimelineActivityResponse::title)
				.containsExactly("남은 활동");
		assertThat(timeline.get(0).records())
				.extracting(TimelineRecordResponse::title)
				.containsExactly("남은 기록");
	}

	@Test
	@DisplayName("다른 사용자의 활동은 조회되지 않는다")
	void otherUsersActivityIsNotReturned() {
		User owner = createUser();
		User other = createUser();
		createActivity(owner, "내 활동", LocalDate.now().minusDays(5), true);

		assertThat(storyService.getTimeline(other.getId())).isEmpty();
	}

	// createdAt은 @CreatedDate + updatable = false라 JPA로 수정할 수 없어 네이티브 쿼리로 조정한다
	private void setCreatedAt(UUID recordId, LocalDateTime createdAt) {
		transactionTemplate.executeWithoutResult(status ->
				entityManager.createNativeQuery("update records set created_at = :createdAt where id = :id")
						.setParameter("createdAt", createdAt)
						.setParameter("id", recordId)
						.executeUpdate());
	}

	private User createUser() {
		return userRepository.save(User.of(
				UUID.randomUUID() + "@test.com",
				"encoded-password",
				"테스터"
		));
	}

	private Activity createActivity(User user, String title, LocalDate startedAt, boolean archived) {
		ActivityType activityType = activityTypeRepository.save(ActivityType.create(user, "동아리"));
		Activity activity = Activity.create(
				user,
				activityType,
				title,
				"설명",
				startedAt,
				startedAt.plusDays(1),
				false
		);

		if (archived) {
			activity.archive();
		}

		return activityRepository.save(activity);
	}

	private Template createTemplate(User user) {
		return templateRepository.save(Template.createCustom(user, "템플릿", null));
	}

	private Record createRecord(User user, Activity activity, Template template, String title) {
		return recordRepository.save(Record.builder()
				.user(user)
				.activity(activity)
				.template(template)
				.title(title)
				.build());
	}
}
