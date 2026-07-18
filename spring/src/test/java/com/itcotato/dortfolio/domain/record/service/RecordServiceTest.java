package com.itcotato.dortfolio.domain.record.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.itcotato.dortfolio.domain.activity.entity.Activity;
import com.itcotato.dortfolio.domain.activity.entity.ActivityType;
import com.itcotato.dortfolio.domain.activity.repository.ActivityRepository;
import com.itcotato.dortfolio.domain.activity.repository.ActivityTypeRepository;
import com.itcotato.dortfolio.domain.memo.entity.Memo;
import com.itcotato.dortfolio.domain.memo.repository.MemoRepository;
import com.itcotato.dortfolio.domain.record.dto.req.RecordAnswerRequest;
import com.itcotato.dortfolio.domain.record.dto.req.RecordCreateRequest;
import com.itcotato.dortfolio.domain.record.dto.req.RecordMemoRequest;
import com.itcotato.dortfolio.domain.record.dto.req.RecordUpdateRequest;
import com.itcotato.dortfolio.domain.record.dto.res.RecordAnswerResponse;
import com.itcotato.dortfolio.domain.record.dto.res.RecordResponse;
import com.itcotato.dortfolio.domain.record.entity.RecordStatus;
import com.itcotato.dortfolio.domain.record.repository.RecordAnswerRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordMemoRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordRepository;
import com.itcotato.dortfolio.domain.template.entity.ActivityTemplate;
import com.itcotato.dortfolio.domain.template.entity.Template;
import com.itcotato.dortfolio.domain.template.entity.TemplateQuestion;
import com.itcotato.dortfolio.domain.template.repository.ActivityTemplateRepository;
import com.itcotato.dortfolio.domain.template.repository.TemplateRepository;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.ErrorCode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class RecordServiceTest {

	@Autowired
	private RecordService recordService;

	@Autowired
	private RecordRepository recordRepository;

	@Autowired
	private RecordAnswerRepository recordAnswerRepository;

	@Autowired
	private RecordMemoRepository recordMemoRepository;

	@Autowired
	private MemoRepository memoRepository;

	@Autowired
	private TemplateRepository templateRepository;

	@Autowired
	private ActivityTemplateRepository activityTemplateRepository;

	@Autowired
	private ActivityRepository activityRepository;

	@Autowired
	private ActivityTypeRepository activityTypeRepository;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void setUp() {
		recordMemoRepository.deleteAll();
		recordAnswerRepository.deleteAll();
		recordRepository.deleteAll();
		memoRepository.deleteAll();
		activityTemplateRepository.deleteAll();
		templateRepository.deleteAll();
		activityRepository.deleteAll();
		activityTypeRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void createDraftRecordWithAnswersAndMemos() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", true);
		connectTemplate(activity, template);
		Memo memo = createMemo(user, activity);
		TemplateQuestion question = template.getQuestions().get(0);

		RecordResponse response = recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(),
			template.getId(),
			"첫 기록",
			List.of(new RecordAnswerRequest(question.getId(), "상황을 정리했다.")),
			List.of(new RecordMemoRequest(memo.getId(), true)),
			RecordStatus.DRAFT
		));

		assertThat(response.title()).isEqualTo("첫 기록");
		assertThat(response.status()).isEqualTo(RecordStatus.DRAFT.name());
		assertThat(response.answers()).hasSize(1);
		assertThat(response.answers().get(0).answerText()).isEqualTo("상황을 정리했다.");
		assertThat(response.memos()).hasSize(1);
		assertThat(response.memos().get(0).collapsed()).isTrue();
		assertThat(memoRepository.findById(memo.getId()).orElseThrow().getUseCount()).isEqualTo(1);
	}

	@Test
	void createRecordRejectsTemplateNotConnectedToActivity() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", true);

		assertThatThrownBy(() -> recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(),
			template.getId(),
			"첫 기록",
			List.of(),
			List.of(),
			RecordStatus.DRAFT
		)))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.RECORD_TEMPLATE_NOT_CONNECTED);
	}

	@Test
	void createCompleteRecordRejectsMissingRequiredAnswer() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", true);
		connectTemplate(activity, template);

		assertThatThrownBy(() -> recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(),
			template.getId(),
			"첫 기록",
			List.of(),
			List.of(),
			RecordStatus.COMPLETED
		)))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.RECORD_REQUIRED_ANSWER_MISSING);
	}

	@Test
	void updateRecordCanCompleteWithLatestChanges() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", true);
		connectTemplate(activity, template);
		TemplateQuestion question = template.getQuestions().get(0);
		RecordResponse draft = recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(),
			template.getId(),
			"첫 기록",
			List.of(),
			List.of(),
			RecordStatus.DRAFT
		));

		RecordResponse completed = recordService.updateRecord(user.getId(), draft.id(), new RecordUpdateRequest(
			"첫 기록",
			List.of(new RecordAnswerRequest(question.getId(), "답변")),
			List.of(),
			RecordStatus.COMPLETED
		));

		assertThat(completed.status()).isEqualTo(RecordStatus.COMPLETED.name());
		assertThat(completed.completedAt()).isNotNull();
	}

	@Test
	void updateRecordRejectsCompletedToDraftTransition() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", false);
		connectTemplate(activity, template);
		RecordResponse completed = recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(),
			template.getId(),
			"완료 기록",
			List.of(),
			List.of(),
			RecordStatus.COMPLETED
		));

		assertThatThrownBy(() -> recordService.updateRecord(user.getId(), completed.id(), new RecordUpdateRequest(
			"완료 기록 수정",
			List.of(),
			List.of(),
			RecordStatus.DRAFT
		)))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.RECORD_STATUS_TRANSITION_NOT_ALLOWED);

		assertThat(recordService.getRecord(user.getId(), completed.id()).status())
			.isEqualTo(RecordStatus.COMPLETED.name());
	}

	@Test
	void updateRecordRejectsMemoFromDifferentActivity() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Activity otherActivity = createActivity(user, "다른 활동");
		Template template = createTemplate(user, "문제 해결", false);
		connectTemplate(activity, template);
		Memo otherActivityMemo = createMemo(user, otherActivity);
		RecordResponse draft = recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(),
			template.getId(),
			"첫 기록",
			List.of(),
			List.of(),
			RecordStatus.DRAFT
		));

		assertThatThrownBy(() -> recordService.updateRecord(user.getId(), draft.id(), new RecordUpdateRequest(
			"첫 기록",
			List.of(),
			List.of(new RecordMemoRequest(otherActivityMemo.getId(), false)),
			RecordStatus.DRAFT
		)))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.RECORD_MEMO_ACTIVITY_MISMATCH);
	}

	@Test
	void updateRecordKeepsMemoUseCountAsCurrentConnectionCount() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", false);
		connectTemplate(activity, template);
		Memo firstMemo = createMemo(user, activity);
		Memo secondMemo = createMemo(user, activity);
		RecordResponse draft = recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(),
			template.getId(),
			"첫 기록",
			List.of(),
			List.of(new RecordMemoRequest(firstMemo.getId(), false)),
			RecordStatus.DRAFT
		));

		recordService.updateRecord(user.getId(), draft.id(), new RecordUpdateRequest(
			"첫 기록",
			List.of(),
			List.of(new RecordMemoRequest(firstMemo.getId(), true)),
			RecordStatus.DRAFT
		));

		assertThat(memoRepository.findById(firstMemo.getId()).orElseThrow().getUseCount()).isEqualTo(1);

		recordService.updateRecord(user.getId(), draft.id(), new RecordUpdateRequest(
			"첫 기록",
			List.of(),
			List.of(new RecordMemoRequest(secondMemo.getId(), false)),
			RecordStatus.DRAFT
		));

		assertThat(memoRepository.findById(firstMemo.getId()).orElseThrow().getUseCount()).isZero();
		assertThat(memoRepository.findById(secondMemo.getId()).orElseThrow().getUseCount()).isEqualTo(1);
	}

	@Test
	void deleteRecordExcludesFromListAndRestoreBringsBack() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", false);
		connectTemplate(activity, template);
		Memo memo = createMemo(user, activity);
		RecordResponse draft = recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(),
			template.getId(),
			"첫 기록",
			List.of(),
			List.of(new RecordMemoRequest(memo.getId(), false)),
			RecordStatus.DRAFT
		));

		recordService.deleteRecord(user.getId(), draft.id());

		assertThat(memoRepository.findById(memo.getId()).orElseThrow().getUseCount()).isZero();
		assertThat(recordService.getRecords(user.getId(), null, null, null)).isEmpty();
		assertThatThrownBy(() -> recordService.getRecord(user.getId(), draft.id()))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.RECORD_NOT_FOUND);

		recordService.restoreRecord(user.getId(), draft.id());

		assertThat(memoRepository.findById(memo.getId()).orElseThrow().getUseCount()).isEqualTo(1);
		assertThat(recordService.getRecords(user.getId(), null, null, null)).hasSize(1);
	}

	@Test
	void restoreRecordRejectsDeletedActivity() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", false);
		connectTemplate(activity, template);
		RecordResponse draft = recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(),
			template.getId(),
			"첫 기록",
			List.of(),
			List.of(),
			RecordStatus.DRAFT
		));
		recordService.deleteRecord(user.getId(), draft.id());
		activity.markDeleted(30);
		activityRepository.save(activity);

		assertThatThrownBy(() -> recordService.restoreRecord(user.getId(), draft.id()))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.RECORD_RESTORE_NOT_ALLOWED);
	}

	@Test
	void restoreRecordRejectsDeletedMemo() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", false);
		connectTemplate(activity, template);
		Memo memo = createMemo(user, activity);
		RecordResponse draft = recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(),
			template.getId(),
			"첫 기록",
			List.of(),
			List.of(new RecordMemoRequest(memo.getId(), false)),
			RecordStatus.DRAFT
		));
		recordService.deleteRecord(user.getId(), draft.id());
		memo.markDeleted(30);
		memoRepository.save(memo);

		assertThatThrownBy(() -> recordService.restoreRecord(user.getId(), draft.id()))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.RECORD_RESTORE_NOT_ALLOWED);
	}

	@Test
	void restoreRecordRejectsExpiredDeletePendingUntil() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", false);
		connectTemplate(activity, template);
		RecordResponse draft = recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(),
			template.getId(),
			"첫 기록",
			List.of(),
			List.of(),
			RecordStatus.DRAFT
		));
		com.itcotato.dortfolio.domain.record.entity.Record record = recordRepository.findById(draft.id()).orElseThrow();
		record.markDeleted(-1);
		recordRepository.save(record);

		assertThatThrownBy(() -> recordService.restoreRecord(user.getId(), draft.id()))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.RECORD_RESTORE_NOT_ALLOWED);
	}

	@Test
	void getRecordRejectsDeletedActivity() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", false);
		connectTemplate(activity, template);
		RecordResponse draft = recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(),
			template.getId(),
			"첫 기록",
			List.of(),
			List.of(),
			RecordStatus.DRAFT
		));
		activity.markDeleted(30);
		activityRepository.save(activity);

		assertThatThrownBy(() -> recordService.getRecord(user.getId(), draft.id()))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.RECORD_NOT_FOUND);
	}

	@Test
	void recordAnswerKeepsQuestionSnapshotAfterTemplateQuestionReplacement() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", true);
		connectTemplate(activity, template);
		TemplateQuestion question = template.getQuestions().get(0);
		RecordResponse draft = recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(),
			template.getId(),
			"첫 기록",
			List.of(new RecordAnswerRequest(question.getId(), "기존 답변")),
			List.of(),
			RecordStatus.DRAFT
		));

		template.replaceQuestions(List.of(TemplateQuestion.create("새 질문", "새 설명", false, 1)));
		templateRepository.saveAndFlush(template);

		RecordResponse response = recordService.getRecord(user.getId(), draft.id());

		assertThat(response.answers())
			.extracting(
				RecordAnswerResponse::templateQuestionId,
				RecordAnswerResponse::questionText,
				RecordAnswerResponse::answerText
			)
			.containsExactly(tuple(question.getId(), "질문", "기존 답변"));
	}

	@Test
	void updateCompletedRecordValidatesRequiredAnswersBySnapshotQuestions() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", false);
		connectTemplate(activity, template);
		TemplateQuestion question = template.getQuestions().get(0);
		RecordResponse completed = recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(),
			template.getId(),
			"완료 기록",
			List.of(new RecordAnswerRequest(question.getId(), "기존 답변")),
			List.of(),
			RecordStatus.COMPLETED
		));
		template.replaceQuestions(List.of(TemplateQuestion.create("새 필수 질문", "새 설명", true, 1)));
		templateRepository.saveAndFlush(template);

		RecordResponse updated = recordService.updateRecord(user.getId(), completed.id(), new RecordUpdateRequest(
			"완료 기록 수정",
			List.of(new RecordAnswerRequest(question.getId(), "수정 답변")),
			List.of(),
			RecordStatus.COMPLETED
		));

		assertThat(updated.status()).isEqualTo(RecordStatus.COMPLETED.name());
		assertThat(updated.answers())
			.extracting(
				RecordAnswerResponse::templateQuestionId,
				RecordAnswerResponse::questionText,
				RecordAnswerResponse::answerText
			)
			.containsExactly(tuple(question.getId(), "질문", "수정 답변"));
	}

	@Test
	void getRecordsFiltersByActivityTemplateAndStatus() {
		User user = createUser();
		Activity firstActivity = createActivity(user, "첫 활동");
		Activity secondActivity = createActivity(user, "두 번째 활동");
		Template firstTemplate = createTemplate(user, "문제 해결", false);
		Template secondTemplate = createTemplate(user, "협업", false);
		connectTemplate(firstActivity, firstTemplate);
		connectTemplate(firstActivity, secondTemplate);
		connectTemplate(secondActivity, firstTemplate);

		RecordResponse firstDraft = recordService.createRecord(user.getId(), new RecordCreateRequest(
			firstActivity.getId(),
			firstTemplate.getId(),
			"첫 draft",
			List.of(),
			List.of(),
			RecordStatus.DRAFT
		));
		RecordResponse secondDraft = recordService.createRecord(user.getId(), new RecordCreateRequest(
			firstActivity.getId(),
			secondTemplate.getId(),
			"두 번째 draft",
			List.of(),
			List.of(),
			RecordStatus.DRAFT
		));
		RecordResponse completed = recordService.createRecord(user.getId(), new RecordCreateRequest(
			secondActivity.getId(),
			firstTemplate.getId(),
			"완료 기록",
			List.of(),
			List.of(),
			RecordStatus.COMPLETED
		));

		assertThat(recordService.getRecords(user.getId(), firstActivity.getId(), null, null))
			.extracting(record -> record.id())
			.containsExactlyInAnyOrder(firstDraft.id(), secondDraft.id());
		assertThat(recordService.getRecords(user.getId(), firstActivity.getId(), secondTemplate.getId(), RecordStatus.DRAFT))
			.extracting(record -> record.title())
			.containsExactly("두 번째 draft");
		assertThat(recordService.getRecords(user.getId(), null, null, RecordStatus.COMPLETED))
			.extracting(record -> record.id())
			.containsExactly(completed.id());
		assertThat(recordService.getRecords(user.getId(), null, null, RecordStatus.COMPLETED))
			.extracting(record -> record.title())
			.containsExactly("완료 기록");
	}

	private User createUser() {
		return userRepository.save(User.of(
			UUID.randomUUID() + "@test.com",
			"encoded-password",
			"테스터",
			true,
			true,
			false
		));
	}

	private Activity createActivity(User user, String title) {
		ActivityType activityType = activityTypeRepository.save(ActivityType.create(user, "프로젝트"));
		return activityRepository.save(Activity.create(
			user,
			activityType,
			title,
			"설명",
			LocalDate.now(),
			null,
			true
		));
	}

	private Template createTemplate(User user, String title, boolean required) {
		Template template = Template.createCustom(user, title, "설명");
		template.addQuestion(TemplateQuestion.create("질문", "설명", required, 1));
		return templateRepository.save(template);
	}

	private Memo createMemo(User user, Activity activity) {
		return memoRepository.save(Memo.create(user, activity, "메모", "내용", null, 1));
	}

	private void connectTemplate(Activity activity, Template template) {
		activityTemplateRepository.save(ActivityTemplate.create(activity, template, 1));
	}
}
