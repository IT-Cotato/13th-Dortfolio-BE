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
import com.itcotato.dortfolio.domain.record.analysis.entity.RecordAnalysis;
import com.itcotato.dortfolio.domain.record.analysis.repository.RecordAnalysisRepository;
import com.itcotato.dortfolio.domain.record.dto.req.RecordAnswerRequest;
import com.itcotato.dortfolio.domain.record.dto.req.RecordCreateRequest;
import com.itcotato.dortfolio.domain.record.dto.req.RecordMemoRequest;
import com.itcotato.dortfolio.domain.record.dto.req.RecordUpdateRequest;
import com.itcotato.dortfolio.domain.record.dto.res.RecordAnswerResponse;
import com.itcotato.dortfolio.domain.record.dto.res.RecordPageResponse;
import com.itcotato.dortfolio.domain.record.dto.res.RecordResponse;
import com.itcotato.dortfolio.domain.record.dto.res.RecordSummaryResponse;
import com.itcotato.dortfolio.domain.record.entity.CompetencyTag;
import com.itcotato.dortfolio.domain.record.entity.RecordCompetencyTag;
import com.itcotato.dortfolio.domain.record.entity.RecordStatus;
import com.itcotato.dortfolio.domain.record.repository.CompetencyTagRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordAnswerRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordCompetencyTagRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordEmbeddingRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordMemoRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordRepository;
import com.itcotato.dortfolio.domain.template.entity.Template;
import com.itcotato.dortfolio.domain.template.entity.TemplateQuestion;
import com.itcotato.dortfolio.domain.template.repository.TemplateRepository;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.RecordErrorCode;
import com.itcotato.dortfolio.global.exception.types.TemplateErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@ActiveProfiles("test")
@SpringBootTest
class RecordServiceTest {

	@Autowired
	private RecordService recordService;

	@Autowired
	private RecordRepository recordRepository;

	@Autowired
	private RecordAnalysisRepository recordAnalysisRepository;

	@Autowired
	private RecordAnswerRepository recordAnswerRepository;

	@Autowired
	private RecordEmbeddingRepository recordEmbeddingRepository;

	@Autowired
	private RecordCompetencyTagRepository recordCompetencyTagRepository;

	@Autowired
	private CompetencyTagRepository competencyTagRepository;

	@Autowired
	private RecordMemoRepository recordMemoRepository;

	@Autowired
	private MemoRepository memoRepository;

	@Autowired
	private TemplateRepository templateRepository;

	@Autowired
	private ActivityRepository activityRepository;

	@Autowired
	private ActivityTypeRepository activityTypeRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

	@BeforeEach
	void setUp() {
		recordAnalysisRepository.deleteAll();
		jdbcTemplate.update("delete from record_embeddings");
		recordCompetencyTagRepository.deleteAll();
		competencyTagRepository.deleteAll();
		recordMemoRepository.deleteAll();
		recordAnswerRepository.deleteAll();
		recordRepository.deleteAll();
		memoRepository.deleteAll();
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
	void createRecordExcludesArchivedTemplateQuestions() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		TemplateQuestion activeQuestion = TemplateQuestion.create("현재 질문", null, false, 1);
		TemplateQuestion archivedQuestion = TemplateQuestion.create("이전 질문", null, true, 2);
		ReflectionTestUtils.setField(archivedQuestion, "deletedAt", LocalDateTime.now());
		Template template = Template.createCustom(user, "문제 해결", null);
		template.initializeQuestions(List.of(activeQuestion, archivedQuestion));
		templateRepository.save(template);

		RecordResponse response = recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(),
			template.getId(),
			"첫 기록",
			List.of(),
			List.of(),
			RecordStatus.DRAFT
		));

		assertThat(response.answers())
			.extracting(RecordAnswerResponse::questionText)
			.containsExactly("현재 질문");
	}

	@Test
	void createRecordAllowsOwnedTemplateWithoutActivityConnection() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", true);

		RecordResponse response = recordService.createRecord(user.getId(), new RecordCreateRequest(
				activity.getId(),
				template.getId(),
				"첫 기록",
				List.of(),
				List.of(),
				RecordStatus.DRAFT
		));

		assertThat(response.templateId()).isEqualTo(template.getId());
	}

	@Test
	void createRecordRejectsDeletedTemplate() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", false);
		template.delete();
		templateRepository.saveAndFlush(template);

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
				.isEqualTo(TemplateErrorCode.TEMPLATE_NOT_FOUND);
	}

	@Test
	void createCompleteRecordRejectsMissingRequiredAnswer() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", true);

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
					.isEqualTo(RecordErrorCode.RECORD_REQUIRED_ANSWER_MISSING);
	}

	@Test
	void createRecordDefaultsToDraftWhenStatusIsMissing() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", true);

		RecordResponse response = recordService.createRecord(user.getId(), new RecordCreateRequest(
				activity.getId(),
				template.getId(),
				"첫 기록",
				List.of(),
				List.of(),
				null
		));

		assertThat(response.status()).isEqualTo(RecordStatus.DRAFT.name());
	}

	@Test
	void updateRecordCanCompleteWithLatestChanges() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", true);
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
				.isEqualTo(RecordErrorCode.RECORD_STATUS_TRANSITION_NOT_ALLOWED);

		assertThat(recordService.getRecord(user.getId(), completed.id()).status())
				.isEqualTo(RecordStatus.COMPLETED.name());
	}

	@Test
	void updateRecordRejectsMemoFromDifferentActivity() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Activity otherActivity = createActivity(user, "다른 활동");
		Template template = createTemplate(user, "문제 해결", false);
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
				.isEqualTo(RecordErrorCode.RECORD_MEMO_ACTIVITY_MISMATCH);
	}

	@Test
	void updateRecordKeepsMemoUseCountAsCurrentConnectionCount() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", false);
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
				.isEqualTo(RecordErrorCode.RECORD_NOT_FOUND);

		recordService.restoreRecord(user.getId(), draft.id());

		assertThat(memoRepository.findById(memo.getId()).orElseThrow().getUseCount()).isEqualTo(1);
		assertThat(recordService.getRecords(user.getId(), null, null, null)).hasSize(1);
	}

	@Test
	void deleteRecordKeepsAnalysisDataForRestore() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", false);
		RecordResponse completed = recordService.createRecord(user.getId(), new RecordCreateRequest(
				activity.getId(),
				template.getId(),
				"완료 기록",
				List.of(),
				List.of(),
				RecordStatus.COMPLETED
		));
		com.itcotato.dortfolio.domain.record.entity.Record record = recordRepository.findById(completed.id()).orElseThrow();
		recordAnalysisRepository.save(RecordAnalysis.pending(record));
		insertRecordEmbedding(record.getId());
		CompetencyTag competencyTag = competencyTagRepository.save(CompetencyTag.create("문제 해결", "문제를 해결한 역량"));
		recordCompetencyTagRepository.save(RecordCompetencyTag.create(record, competencyTag, 0.9f));

		recordService.deleteRecord(user.getId(), completed.id());

		assertThat(recordAnalysisRepository.findByRecord_Id(completed.id())).isPresent();
		assertThat(recordEmbeddingRepository.countByRecord_Id(completed.id())).isEqualTo(1);
		assertThat(recordCompetencyTagRepository.findAllByRecord_Id(completed.id())).hasSize(1);

		recordService.restoreRecord(user.getId(), completed.id());

		assertThat(recordAnalysisRepository.findByRecord_Id(completed.id())).isPresent();
		assertThat(recordEmbeddingRepository.countByRecord_Id(completed.id())).isEqualTo(1);
		assertThat(recordCompetencyTagRepository.findAllByRecord_Id(completed.id())).hasSize(1);
	}

	@Test
	void permanentlyDeleteRecordRemovesRecordAndChildren() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", false);
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
		com.itcotato.dortfolio.domain.record.entity.Record record = recordRepository.findById(draft.id()).orElseThrow();
		recordAnalysisRepository.save(RecordAnalysis.pending(record));
		insertRecordEmbedding(record.getId());
		CompetencyTag competencyTag = competencyTagRepository.save(CompetencyTag.create("문제 해결", "문제를 해결한 역량"));
		recordCompetencyTagRepository.save(RecordCompetencyTag.create(record, competencyTag, 0.9f));

		recordService.permanentlyDeleteRecord(user.getId(), draft.id());

		assertThat(recordRepository.findById(draft.id())).isEmpty();
		assertThat(recordAnswerRepository.findAllByRecord_IdOrderByTemplateQuestion_SortOrderAsc(draft.id())).isEmpty();
		assertThat(recordMemoRepository.findAllByRecord_IdOrderBySortOrderAsc(draft.id())).isEmpty();
		assertThat(recordAnalysisRepository.findByRecord_Id(draft.id())).isEmpty();
		assertThat(recordEmbeddingRepository.countByRecord_Id(draft.id())).isZero();
		assertThat(recordCompetencyTagRepository.findAllByRecord_Id(draft.id())).isEmpty();
		assertThat(memoRepository.findById(memo.getId()).orElseThrow().getUseCount()).isZero();
	}

	@Test
	void permanentlyDeleteRecordRejectsActiveRecord() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", false);
		RecordResponse draft = recordService.createRecord(user.getId(), new RecordCreateRequest(
				activity.getId(),
				template.getId(),
				"첫 기록",
				List.of(),
				List.of(),
				RecordStatus.DRAFT
		));

		assertThatThrownBy(() -> recordService.permanentlyDeleteRecord(user.getId(), draft.id()))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode")
				.isEqualTo(RecordErrorCode.RECORD_PERMANENT_DELETE_NOT_ALLOWED);
		assertThat(recordRepository.findById(draft.id())).isPresent();
	}

	@Test
	void restoreRecordRejectsDeletedActivity() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", false);
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
				.isEqualTo(RecordErrorCode.RECORD_RESTORE_NOT_ALLOWED);
	}

	@Test
	void restoreRecordRejectsExpiredDeletePendingUntil() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", false);
		RecordResponse draft = recordService.createRecord(user.getId(), new RecordCreateRequest(
				activity.getId(),
				template.getId(),
				"첫 기록",
				List.of(),
				List.of(),
				RecordStatus.DRAFT
		));
		com.itcotato.dortfolio.domain.record.entity.Record record = recordRepository.findById(draft.id()).orElseThrow();
		expireDeletePendingWindow(record);
		recordRepository.save(record);

		assertThatThrownBy(() -> recordService.restoreRecord(user.getId(), draft.id()))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode")
				.isEqualTo(RecordErrorCode.RECORD_RESTORE_NOT_ALLOWED);
	}

	@Test
	void restoreRecordAllowsDeletedTemplate() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", false);
		RecordResponse draft = recordService.createRecord(user.getId(), new RecordCreateRequest(
				activity.getId(),
				template.getId(),
				"첫 기록",
				List.of(),
				List.of(),
				RecordStatus.DRAFT
		));
		recordService.deleteRecord(user.getId(), draft.id());
		template.delete();
		templateRepository.save(template);

		recordService.restoreRecord(user.getId(), draft.id());

		assertThat(recordService.getRecord(user.getId(), draft.id()).id()).isEqualTo(draft.id());
	}

	@Test
	void getRecordRejectsDeletedActivity() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", false);
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
				.isEqualTo(RecordErrorCode.RECORD_NOT_FOUND);
	}

	@Test
	void getRecordAllowsDeletedTemplate() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", true);
		TemplateQuestion question = template.getQuestions().get(0);
		RecordResponse draft = recordService.createRecord(user.getId(), new RecordCreateRequest(
				activity.getId(),
				template.getId(),
				"첫 기록",
				List.of(new RecordAnswerRequest(question.getId(), "기존 답변")),
				List.of(),
				RecordStatus.DRAFT
		));

		template.delete();
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
	void updateRecordAllowsDeletedTemplate() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", false);
		TemplateQuestion question = template.getQuestions().get(0);
		RecordResponse completed = recordService.createRecord(user.getId(), new RecordCreateRequest(
				activity.getId(),
				template.getId(),
				"완료 기록",
				List.of(new RecordAnswerRequest(question.getId(), "기존 답변")),
				List.of(),
				RecordStatus.COMPLETED
		));
		template.delete();
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
	void getRecordsFiltersByActivityAndTemplateAndStatus() {
		User user = createUser();
		Activity firstActivity = createActivity(user, "첫 활동");
		Activity secondActivity = createActivity(user, "두 번째 활동");
		Template firstTemplate = createTemplate(user, "문제 해결", false);
		Template secondTemplate = createTemplate(user, "협업", false);

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
		secondTemplate.delete();
		templateRepository.saveAndFlush(secondTemplate);

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

	@Test
	void getRecordPageReturnsSevenRecordsPerPageByDefault() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", false);
		for (int index = 1; index <= 8; index++) {
			recordService.createRecord(user.getId(), new RecordCreateRequest(
					activity.getId(),
					template.getId(),
					"기록 " + index,
					List.of(),
					List.of(),
					RecordStatus.DRAFT
			));
		}

		RecordPageResponse firstPage = recordService.getRecordPage(user.getId(), null, null, null, 0, null);
		RecordPageResponse secondPage = recordService.getRecordPage(user.getId(), null, null, null, 1, null);

		assertThat(firstPage.content()).hasSize(7);
		assertThat(firstPage.totalElements()).isEqualTo(8);
		assertThat(firstPage.totalPages()).isEqualTo(2);
		assertThat(firstPage.first()).isTrue();
		assertThat(firstPage.last()).isFalse();
		assertThat(secondPage.content()).hasSize(1);
		assertThat(secondPage.last()).isTrue();
	}

	@Test
	void getRecordPageRejectsSizeGreaterThanMaxPageSize() {
		User user = createUser();

		assertThatThrownBy(() -> recordService.getRecordPage(user.getId(), null, null, null, 0, 51))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode")
				.isEqualTo(RecordErrorCode.RECORD_INVALID_PAGE_REQUEST);
	}

	@Test
	void getRecordPageAndRecentRecordsKeepCreatedAtNewestFirstAfterOlderRecordUpdate() {
		User user = createUser();
		Activity activity = createActivity(user, "도트폴리오");
		Template template = createTemplate(user, "문제 해결", false);
		RecordResponse older = recordService.createRecord(user.getId(), new RecordCreateRequest(
				activity.getId(),
				template.getId(),
				"먼저 작성한 기록",
				List.of(),
				List.of(),
				RecordStatus.DRAFT
		));
		RecordResponse newer = recordService.createRecord(user.getId(), new RecordCreateRequest(
				activity.getId(),
				template.getId(),
				"나중에 작성한 기록",
				List.of(),
				List.of(),
				RecordStatus.DRAFT
		));
		LocalDateTime baseTime = LocalDateTime.now().minusDays(1);
		setRecordTimestamps(older.id(), baseTime, baseTime);
		setRecordTimestamps(newer.id(), baseTime.plusMinutes(1), baseTime.plusMinutes(1));

		recordService.updateRecord(user.getId(), older.id(), new RecordUpdateRequest(
				"먼저 작성한 기록 수정",
				List.of(),
				List.of(),
				RecordStatus.DRAFT
		));

		assertThat(recordService.getRecordPage(user.getId(), null, null, null, 0, null).content())
				.extracting(RecordSummaryResponse::id)
				.containsExactly(newer.id(), older.id());
		assertThat(recordService.getRecords(user.getId(), null, null, null))
				.extracting(RecordSummaryResponse::id)
				.containsExactly(newer.id(), older.id());
		assertThat(recordService.getRecentRecords(user.getId()))
				.extracting(RecordSummaryResponse::id)
				.containsExactly(newer.id(), older.id());
	}

	private User createUser() {
		return userRepository.save(User.of(
				UUID.randomUUID() + "@test.com",
				"encoded-password",
				"테스터"
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
		template.initializeQuestions(List.of(TemplateQuestion.create("질문", "설명", required, 1)));
		return templateRepository.save(template);
	}

	private Memo createMemo(User user, Activity activity) {
		return memoRepository.save(Memo.create(user, activity, "메모", "내용", 1));
	}

	private void expireDeletePendingWindow(com.itcotato.dortfolio.domain.record.entity.Record record) {
		ReflectionTestUtils.setField(record, "deletedAt", LocalDateTime.now().minusDays(31));
		ReflectionTestUtils.setField(record, "deletePendingUntil", LocalDateTime.now().minusDays(1));
	}

	private void insertRecordEmbedding(UUID recordId) {
		jdbcTemplate.update("""
				insert into record_embeddings
					(id, created_at, updated_at, record_id, embedding_model, embedding)
				values
					(?, current_timestamp, current_timestamp, ?, ?, ARRAY[0.1, 0.2])
				""", UUID.randomUUID(), recordId, "text-embedding-3-small");
	}

	private void setRecordTimestamps(UUID recordId, LocalDateTime createdAt, LocalDateTime updatedAt) {
		recordRepository.flush();
		jdbcTemplate.update("""
				update records
				set created_at = ?, updated_at = ?
				where id = ?
				""", createdAt, updatedAt, recordId);
		entityManager.clear();
	}
}
