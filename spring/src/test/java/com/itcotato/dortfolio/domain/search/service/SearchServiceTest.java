package com.itcotato.dortfolio.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.itcotato.dortfolio.domain.activity.entity.Activity;
import com.itcotato.dortfolio.domain.activity.entity.ActivityType;
import com.itcotato.dortfolio.domain.activity.repository.ActivityRepository;
import com.itcotato.dortfolio.domain.activity.repository.ActivityTypeRepository;
import com.itcotato.dortfolio.domain.record.analysis.repository.RecordAnalysisRepository;
import com.itcotato.dortfolio.domain.record.entity.Record;
import com.itcotato.dortfolio.domain.record.entity.RecordAnswer;
import com.itcotato.dortfolio.domain.record.repository.CompetencyTagRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordAnswerRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordCompetencyTagRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordMemoRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordRepository;
import com.itcotato.dortfolio.domain.search.dto.res.RecordSearchPageResponse;
import com.itcotato.dortfolio.domain.search.dto.res.RecordSearchResponse;
import com.itcotato.dortfolio.domain.template.entity.Template;
import com.itcotato.dortfolio.domain.template.entity.TemplateQuestion;
import com.itcotato.dortfolio.domain.template.repository.TemplateRepository;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.SearchErrorCode;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class SearchServiceTest {

	@Autowired
	private SearchService searchService;

	@Autowired
	private ActivityRepository activityRepository;

	@Autowired
	private ActivityTypeRepository activityTypeRepository;

	@Autowired
	private RecordRepository recordRepository;

	@Autowired
	private RecordAnswerRepository recordAnswerRepository;

	@Autowired
	private TemplateRepository templateRepository;

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
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		cleanUp();
	}

	// 다른 테스트 클래스에 데이터를 남기지 않도록 실행 후에도 정리한다
	@AfterEach
	void tearDown() {
		cleanUp();
	}

	private void cleanUp() {
		recordAnalysisRepository.deleteAll();
		jdbcTemplate.update("delete from record_embeddings");
		recordCompetencyTagRepository.deleteAll();
		competencyTagRepository.deleteAll();
		recordMemoRepository.deleteAll();
		recordAnswerRepository.deleteAll();
		recordRepository.deleteAll();
		templateRepository.deleteAll();
		activityRepository.deleteAll();
		activityTypeRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	@DisplayName("기록 제목에 키워드가 포함되면 검색된다")
	void searchByRecordTitle() {
		Fixture fixture = createFixture();
		createRecord(fixture, "면접 준비 회고", "내용");

		RecordSearchPageResponse result = searchService.searchRecords(fixture.userId(), "면접", 0, null);

		assertThat(result.totalElements()).isEqualTo(1);
		assertThat(result.content())
				.extracting(RecordSearchResponse::title)
				.containsExactly("면접 준비 회고");
	}

	@Test
	@DisplayName("기록 내용에 키워드가 포함되면 검색된다")
	void searchByAnswerText() {
		Fixture fixture = createFixture();
		createRecord(fixture, "제목", "팀원과 갈등을 해결한 경험");

		RecordSearchPageResponse result = searchService.searchRecords(fixture.userId(), "갈등", 0, null);

		assertThat(result.totalElements()).isEqualTo(1);
		assertThat(result.content().get(0).content()).isEqualTo("팀원과 갈등을 해결한 경험");
	}

	@Test
	@DisplayName("삭제된 템플릿으로 작성한 기존 기록도 검색된다")
	void searchRecordUsingDeletedTemplate() {
		Fixture fixture = createFixture();
		createRecord(fixture, "면접 준비 회고", "내용");
		fixture.template().delete();
		templateRepository.saveAndFlush(fixture.template());

		RecordSearchPageResponse result = searchService.searchRecords(fixture.userId(), "면접", 0, null);

		assertThat(result.content())
				.extracting(RecordSearchResponse::title)
				.containsExactly("면접 준비 회고");
	}

	@Test
	@DisplayName("어절 중간의 형태소도 검색된다")
	void searchMatchesInsideWord() {
		Fixture fixture = createFixture();
		createRecord(fixture, "제목", "면접을 준비했다");

		RecordSearchPageResponse result = searchService.searchRecords(fixture.userId(), "면접", 0, null);

		assertThat(result.totalElements()).isEqualTo(1);
	}

	@Test
	@DisplayName("제목과 내용 모두 일치해도 기록은 한 건으로 집계된다")
	void recordIsNotDuplicatedWhenBothMatch() {
		Fixture fixture = createFixture();
		Record record = createRecord(fixture, "협업 경험", "협업을 잘했다");
		saveAnswer(record, "협업이 중요하다", 2);

		RecordSearchPageResponse result = searchService.searchRecords(fixture.userId(), "협업", 0, null);

		assertThat(result.totalElements()).isEqualTo(1);
		assertThat(result.content()).hasSize(1);
	}

	@Test
	@DisplayName("검색 결과에 활동 종류와 템플릿 제목이 포함된다")
	void searchResultIncludesTags() {
		Fixture fixture = createFixture();
		createRecord(fixture, "제목", "내용");

		RecordSearchResponse response = searchService.searchRecords(fixture.userId(), "제목", 0, null)
				.content().get(0);

		assertThat(response.activityTypeName()).isEqualTo("동아리");
		assertThat(response.templateTitle()).isEqualTo("템플릿");
		assertThat(response.createdAt()).isNotNull();
	}

	@Test
	@DisplayName("키워드가 포함된 답변을 우선해서 기록 내용으로 보여준다")
	void picksMatchingAnswerAsContent() {
		Fixture fixture = createFixture();
		Record record = createRecord(fixture, "제목", "첫 번째 답변");
		saveAnswer(record, "리더십을 발휘한 경험", 2);

		RecordSearchResponse response = searchService.searchRecords(fixture.userId(), "리더십", 0, null)
				.content().get(0);

		assertThat(response.content()).isEqualTo("리더십을 발휘한 경험");
	}

	@Test
	@DisplayName("태그(활동 종류, 템플릿 제목)는 검색 대상이 아니다")
	void tagsAreNotSearchable() {
		Fixture fixture = createFixture();
		createRecord(fixture, "제목", "내용");

		assertThat(searchService.searchRecords(fixture.userId(), "동아리", 0, null).totalElements()).isZero();
		assertThat(searchService.searchRecords(fixture.userId(), "템플릿", 0, null).totalElements()).isZero();
	}

	@Test
	@DisplayName("자음이나 모음만 검색하면 결과가 0건이다")
	void jamoOnlyKeywordReturnsEmpty() {
		Fixture fixture = createFixture();
		createRecord(fixture, "면접 준비", "면접 내용");

		assertThat(searchService.searchRecords(fixture.userId(), "ㅁ", 0, null).totalElements()).isZero();
		assertThat(searchService.searchRecords(fixture.userId(), "ㅁㅈ", 0, null).totalElements()).isZero();
		assertThat(searchService.searchRecords(fixture.userId(), "ㅏ", 0, null).totalElements()).isZero();
	}

	@Test
	@DisplayName("일치하는 기록이 없으면 결과가 0건이다")
	void noMatchReturnsEmpty() {
		Fixture fixture = createFixture();
		createRecord(fixture, "면접 준비", "면접 내용");

		RecordSearchPageResponse result = searchService.searchRecords(fixture.userId(), "존재하지않는키워드", 0, null);

		assertThat(result.totalElements()).isZero();
		assertThat(result.content()).isEmpty();
	}

	@Test
	@DisplayName("LIKE 와일드카드를 입력해도 문자 그대로 검색된다")
	void wildcardIsEscaped() {
		Fixture fixture = createFixture();
		createRecord(fixture, "면접 준비", "내용");

		assertThat(searchService.searchRecords(fixture.userId(), "%", 0, null).totalElements()).isZero();
		assertThat(searchService.searchRecords(fixture.userId(), "_", 0, null).totalElements()).isZero();
	}

	@Test
	@DisplayName("영문 검색은 대소문자를 구분하지 않는다")
	void searchIsCaseInsensitive() {
		Fixture fixture = createFixture();
		createRecord(fixture, "Spring Boot 학습", "내용");

		assertThat(searchService.searchRecords(fixture.userId(), "spring", 0, null).totalElements()).isEqualTo(1);
		assertThat(searchService.searchRecords(fixture.userId(), "SPRING", 0, null).totalElements()).isEqualTo(1);
	}

	@Test
	@DisplayName("삭제된 기록은 검색되지 않는다")
	void deletedRecordIsNotSearchable() {
		Fixture fixture = createFixture();
		Record record = createRecord(fixture, "면접 준비", "내용");
		record.markDeleted(30);
		recordRepository.save(record);

		assertThat(searchService.searchRecords(fixture.userId(), "면접", 0, null).totalElements()).isZero();
	}

	@Test
	@DisplayName("삭제된 활동에 속한 기록은 검색되지 않는다")
	void recordOfDeletedActivityIsNotSearchable() {
		Fixture fixture = createFixture();
		createRecord(fixture, "면접 준비", "내용");
		fixture.activity().markDeleted(30);
		activityRepository.save(fixture.activity());

		assertThat(searchService.searchRecords(fixture.userId(), "면접", 0, null).totalElements()).isZero();
	}

	@Test
	@DisplayName("다른 사용자의 기록은 검색되지 않는다")
	void otherUsersRecordIsNotSearchable() {
		Fixture fixture = createFixture();
		createRecord(fixture, "면접 준비", "내용");
		User other = createUser();

		assertThat(searchService.searchRecords(other.getId(), "면접", 0, null).totalElements()).isZero();
	}

	@Test
	@DisplayName("페이징 정보가 함께 반환된다")
	void searchIsPaged() {
		Fixture fixture = createFixture();
		createRecord(fixture, "면접 1", "내용");
		createRecord(fixture, "면접 2", "내용");
		createRecord(fixture, "면접 3", "내용");

		RecordSearchPageResponse result = searchService.searchRecords(fixture.userId(), "면접", 0, 2);

		assertThat(result.totalElements()).isEqualTo(3);
		assertThat(result.content()).hasSize(2);
		assertThat(result.totalPages()).isEqualTo(2);
		assertThat(result.first()).isTrue();
		assertThat(result.last()).isFalse();
	}

	@Test
	@DisplayName("잘못된 페이지 요청은 예외가 발생한다")
	void invalidPageRequestThrows() {
		UUID userId = createUser().getId();

		assertThatThrownBy(() -> searchService.searchRecords(userId, "면접", -1, null))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode")
				.isEqualTo(SearchErrorCode.INVALID_PAGE_REQUEST);

		assertThatThrownBy(() -> searchService.searchRecords(userId, "면접", 0, 999))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode")
				.isEqualTo(SearchErrorCode.INVALID_PAGE_REQUEST);
	}

	private record Fixture(UUID userId, User user, Activity activity, Template template) {
	}

	private Fixture createFixture() {
		User user = createUser();
		ActivityType activityType = activityTypeRepository.save(ActivityType.create(user, "동아리"));
		Activity activity = activityRepository.save(Activity.create(
				user, activityType, "활동", "설명",
				LocalDate.now().minusDays(10), LocalDate.now(), false
		));
		Template template = Template.createCustom(user, "템플릿", null);
		template.initializeQuestions(List.of(
				TemplateQuestion.create("질문 1", null, true, 1),
				TemplateQuestion.create("질문 2", null, true, 2)
		));
		templateRepository.save(template);

		return new Fixture(user.getId(), user, activity, template);
	}

	private User createUser() {
		return userRepository.save(User.of(
				UUID.randomUUID() + "@test.com",
				"encoded-password",
				"테스터"
		));
	}

	private Record createRecord(Fixture fixture, String title, String answerText) {
		Record record = recordRepository.save(Record.builder()
				.user(fixture.user())
				.activity(fixture.activity())
				.template(fixture.template())
				.title(title)
				.build());

		saveAnswer(record, answerText, 1);
		return record;
	}

	private void saveAnswer(Record record, String answerText, int sortOrder) {
		recordAnswerRepository.save(RecordAnswer.builder()
				.record(record)
				.templateQuestion(record.getTemplate().getQuestions().get(sortOrder - 1))
				.answerText(answerText)
				.build());
	}

}
