package com.itcotato.dortfolio.domain.record.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.itcotato.dortfolio.domain.activity.entity.Activity;
import com.itcotato.dortfolio.domain.activity.entity.ActivityType;
import com.itcotato.dortfolio.domain.activity.repository.ActivityRepository;
import com.itcotato.dortfolio.domain.activity.repository.ActivityTypeRepository;
import com.itcotato.dortfolio.domain.record.analysis.dto.AnalyzedCompetencyTagResponse;
import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisRequest;
import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisResponse;
import com.itcotato.dortfolio.domain.record.analysis.entity.AiAnalysisStatus;
import com.itcotato.dortfolio.domain.record.analysis.exception.RecordAnalysisErrorCode;
import com.itcotato.dortfolio.domain.record.analysis.repository.RecordAnalysisRepository;
import com.itcotato.dortfolio.domain.record.dto.req.RecordAnswerRequest;
import com.itcotato.dortfolio.domain.record.dto.req.RecordCreateRequest;
import com.itcotato.dortfolio.domain.record.dto.res.RecordResponse;
import com.itcotato.dortfolio.domain.record.entity.CompetencyTag;
import com.itcotato.dortfolio.domain.record.entity.RecordStatus;
import com.itcotato.dortfolio.domain.record.repository.CompetencyTagRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordAnswerRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordCompetencyTagRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordEmbeddingRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordMemoRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordRepository;
import com.itcotato.dortfolio.domain.record.service.RecordService;
import com.itcotato.dortfolio.domain.template.entity.ActivityTemplate;
import com.itcotato.dortfolio.domain.template.entity.Template;
import com.itcotato.dortfolio.domain.template.entity.TemplateQuestion;
import com.itcotato.dortfolio.domain.template.repository.ActivityTemplateRepository;
import com.itcotato.dortfolio.domain.template.repository.TemplateRepository;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@ActiveProfiles("test")
@SpringBootTest
class RecordAnalysisServiceTest {

	@Autowired
	private RecordService recordService;

	@Autowired
	private RecordAnalysisService recordAnalysisService;

	@Autowired
	private StubRecordAnalysisClient stubRecordAnalysisClient;

	@Autowired
	private StubRecordEmbeddingWriter stubRecordEmbeddingWriter;

	@Autowired
	private RecordAnalysisRepository recordAnalysisRepository;

	@Autowired
	private RecordEmbeddingRepository recordEmbeddingRepository;

	@Autowired
	private RecordCompetencyTagRepository recordCompetencyTagRepository;

	@Autowired
	private CompetencyTagRepository competencyTagRepository;

	@Autowired
	private RecordMemoRepository recordMemoRepository;

	@Autowired
	private RecordAnswerRepository recordAnswerRepository;

	@Autowired
	private RecordRepository recordRepository;

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

	@BeforeEach
	void setUp() {
		stubRecordAnalysisClient.reset();
		stubRecordEmbeddingWriter.reset();
		recordAnalysisRepository.deleteAll();
		recordEmbeddingRepository.deleteAll();
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
	void analyzeCompletedRecordSavesAnalysisEmbeddingAndCompetencyTags() {
		User user = createUser();
		Activity activity = createActivity(user);
		Template template = createTemplate(user, true);
		connectTemplate(activity, template);
		CompetencyTag competencyTag = competencyTagRepository.save(CompetencyTag.create("문제 해결", "문제를 해결하는 역량"));
		TemplateQuestion question = template.getQuestions().get(0);
		RecordResponse record = recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(),
			template.getId(),
			"추천 알고리즘 개선",
			List.of(new RecordAnswerRequest(question.getId(), "추천 기준을 다시 정의했습니다.")),
			List.of(),
			RecordStatus.COMPLETED
		));
		stubRecordAnalysisClient.response = new RecordAnalysisResponse(
			"추천 기준을 개선한 경험입니다.",
			List.of("추천 기준을 다시 정의했습니다."),
			List.of(new AnalyzedCompetencyTagResponse(competencyTag.getId(), 0.9f)),
			"test-embedding",
			new float[] {0.1f, 0.2f}
		);

		recordAnalysisService.analyze(record.id());

		assertThat(recordAnalysisRepository.findByRecord_Id(record.id()).orElseThrow().getAiAnalysisStatus())
			.isEqualTo(AiAnalysisStatus.COMPLETED);
		assertThat(recordAnalysisRepository.findByRecord_Id(record.id()).orElseThrow().getSummary())
			.isEqualTo("추천 기준을 개선한 경험입니다.");
		assertThat(recordAnalysisRepository.findByRecord_Id(record.id()).orElseThrow().getEvidenceSnippets())
			.contains("추천 기준을 다시 정의했습니다.");
		assertThat(stubRecordEmbeddingWriter.recordId).isEqualTo(record.id());
		assertThat(stubRecordEmbeddingWriter.embeddingModel).isEqualTo("test-embedding");
		assertThat(stubRecordEmbeddingWriter.embedding).containsExactly(0.1f, 0.2f);
		assertThat(recordCompetencyTagRepository.findAllByRecord_Id(record.id()))
			.singleElement()
			.satisfies(tag -> {
				assertThat(tag.getCompetencyTag().getId()).isEqualTo(competencyTag.getId());
				assertThat(tag.getScore()).isEqualTo(0.9f);
			});
	}

	@Test
	void analyzeStoresFailedStatusWhenClientFails() {
		User user = createUser();
		Activity activity = createActivity(user);
		Template template = createTemplate(user, false);
		connectTemplate(activity, template);
		RecordResponse record = recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(),
			template.getId(),
			"실패 기록",
			List.of(),
			List.of(),
			RecordStatus.COMPLETED
		));
		stubRecordAnalysisClient.failure = new IllegalStateException("AI service unavailable");

		recordAnalysisService.analyze(record.id());

		assertThat(recordAnalysisRepository.findByRecord_Id(record.id()).orElseThrow().getAiAnalysisStatus())
			.isEqualTo(AiAnalysisStatus.FAILED);
		assertThat(recordAnalysisRepository.findByRecord_Id(record.id()).orElseThrow().getFailureReason())
			.contains(RecordAnalysisErrorCode.RECORD_ANALYSIS_AI_SERVICE_FAILED.getCode());
		assertThat(recordAnalysisRepository.findByRecord_Id(record.id()).orElseThrow().isFailureRetryable())
			.isTrue();
		assertThat(recordEmbeddingRepository.countByRecord_Id(record.id())).isZero();
		assertThat(recordCompetencyTagRepository.findAllByRecord_Id(record.id())).isEmpty();
	}

	@Test
	void analyzeStoresRejectedStatusWhenAiServiceReturnsClientError() {
		User user = createUser();
		Activity activity = createActivity(user);
		Template template = createTemplate(user, false);
		connectTemplate(activity, template);
		RecordResponse record = recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(),
			template.getId(),
			"요청 오류 기록",
			List.of(),
			List.of(),
			RecordStatus.COMPLETED
		));
		stubRecordAnalysisClient.failure = HttpClientErrorException.create(
			HttpStatus.UNPROCESSABLE_ENTITY,
			"Unprocessable Content",
			HttpHeaders.EMPTY,
			"body required".getBytes(),
			null
		);

		recordAnalysisService.analyze(record.id());

		assertThat(recordAnalysisRepository.findByRecord_Id(record.id()).orElseThrow().getFailureReason())
			.contains(RecordAnalysisErrorCode.RECORD_ANALYSIS_AI_SERVICE_REJECTED.getCode());
		assertThat(recordAnalysisRepository.findByRecord_Id(record.id()).orElseThrow().isFailureRetryable())
			.isFalse();
	}

	@Test
	void analyzeStoresRetryableFailureWhenAiServiceReturnsServerError() {
		User user = createUser();
		Activity activity = createActivity(user);
		Template template = createTemplate(user, false);
		connectTemplate(activity, template);
		RecordResponse record = recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(),
			template.getId(),
			"서버 오류 기록",
			List.of(),
			List.of(),
			RecordStatus.COMPLETED
		));
		stubRecordAnalysisClient.failure = HttpServerErrorException.create(
			HttpStatus.INTERNAL_SERVER_ERROR,
			"Internal Server Error",
			HttpHeaders.EMPTY,
			"temporary failure".getBytes(),
			null
		);

		recordAnalysisService.analyze(record.id());

		assertThat(recordAnalysisRepository.findByRecord_Id(record.id()).orElseThrow().getFailureReason())
			.contains(RecordAnalysisErrorCode.RECORD_ANALYSIS_AI_SERVICE_FAILED.getCode());
		assertThat(recordAnalysisRepository.findByRecord_Id(record.id()).orElseThrow().isFailureRetryable())
			.isTrue();
	}

	@Test
	void failedReanalysisClearsPreviousAnalysisData() {
		User user = createUser();
		Activity activity = createActivity(user);
		Template template = createTemplate(user, false);
		connectTemplate(activity, template);
		CompetencyTag competencyTag = competencyTagRepository.save(CompetencyTag.create("문제 해결", "문제를 해결하는 역량"));
		RecordResponse record = recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(),
			template.getId(),
			"재분석 기록",
			List.of(),
			List.of(),
			RecordStatus.COMPLETED
		));
		stubRecordAnalysisClient.response = new RecordAnalysisResponse(
			"기존 요약",
			List.of("기존 근거"),
			List.of(new AnalyzedCompetencyTagResponse(competencyTag.getId(), 0.9f)),
			"test-embedding",
			new float[] {0.1f}
		);
		recordAnalysisService.analyze(record.id());
		stubRecordAnalysisClient.reset();
		stubRecordAnalysisClient.failure = new IllegalStateException("AI service unavailable");

		recordAnalysisService.analyze(record.id());

		assertThat(recordAnalysisRepository.findByRecord_Id(record.id()).orElseThrow())
			.satisfies(recordAnalysis -> {
				assertThat(recordAnalysis.getAiAnalysisStatus()).isEqualTo(AiAnalysisStatus.FAILED);
				assertThat(recordAnalysis.getSummary()).isNull();
				assertThat(recordAnalysis.getEvidenceSnippets()).isNull();
			});
		assertThat(recordCompetencyTagRepository.findAllByRecord_Id(record.id())).isEmpty();
	}

	@Test
	void analyzeStoresFailedStatusWhenResponseHasDuplicateCompetencyTags() {
		User user = createUser();
		Activity activity = createActivity(user);
		Template template = createTemplate(user, false);
		connectTemplate(activity, template);
		CompetencyTag competencyTag = competencyTagRepository.save(CompetencyTag.create("협업", "함께 일하는 역량"));
		RecordResponse record = recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(),
			template.getId(),
			"중복 태그 기록",
			List.of(),
			List.of(),
			RecordStatus.COMPLETED
		));
		stubRecordAnalysisClient.response = new RecordAnalysisResponse(
			"요약",
			List.of("근거"),
			List.of(
				new AnalyzedCompetencyTagResponse(competencyTag.getId(), 0.8f),
				new AnalyzedCompetencyTagResponse(competencyTag.getId(), 0.7f)
			),
			"test-embedding",
			new float[] {0.1f}
		);

		recordAnalysisService.analyze(record.id());

		assertThat(recordAnalysisRepository.findByRecord_Id(record.id()).orElseThrow())
			.satisfies(recordAnalysis -> {
				assertThat(recordAnalysis.getAiAnalysisStatus()).isEqualTo(AiAnalysisStatus.FAILED);
				assertThat(recordAnalysis.getFailureReason())
					.contains(RecordAnalysisErrorCode.RECORD_ANALYSIS_INVALID_RESPONSE.getCode());
			});
		assertThat(recordCompetencyTagRepository.findAllByRecord_Id(record.id())).isEmpty();
	}

	@Test
	void analyzeDoesNotSaveResultWhenRecordIsDeletedBeforePersistingResponse() {
		User user = createUser();
		Activity activity = createActivity(user);
		Template template = createTemplate(user, false);
		connectTemplate(activity, template);
		RecordResponse record = recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(),
			template.getId(),
			"삭제 중 분석 기록",
			List.of(),
			List.of(),
			RecordStatus.COMPLETED
		));
		stubRecordAnalysisClient.response = new RecordAnalysisResponse(
			"요약",
			List.of("근거"),
			List.of(),
			"test-embedding",
			new float[] {0.1f}
		);
		stubRecordAnalysisClient.beforeReturn = () -> recordService.deleteRecord(user.getId(), record.id());

		recordAnalysisService.analyze(record.id());

		assertThat(recordAnalysisRepository.findByRecord_Id(record.id())).isEmpty();
		assertThat(recordCompetencyTagRepository.findAllByRecord_Id(record.id())).isEmpty();
		assertThat(stubRecordEmbeddingWriter.recordId).isNull();
	}

	@Test
	void analyzeStoresFailedStatusWhenEmbeddingPersistenceFails() {
		User user = createUser();
		Activity activity = createActivity(user);
		Template template = createTemplate(user, false);
		connectTemplate(activity, template);
		RecordResponse record = recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(),
			template.getId(),
			"저장 실패 기록",
			List.of(),
			List.of(),
			RecordStatus.COMPLETED
		));
		stubRecordAnalysisClient.response = new RecordAnalysisResponse(
			"요약",
			List.of("근거"),
			List.of(),
			"test-embedding",
			new float[] {0.1f}
		);
		stubRecordEmbeddingWriter.failure = new IllegalStateException("embedding write failed");

		recordAnalysisService.analyze(record.id());

		assertThat(recordAnalysisRepository.findByRecord_Id(record.id()).orElseThrow())
			.satisfies(recordAnalysis -> {
				assertThat(recordAnalysis.getAiAnalysisStatus()).isEqualTo(AiAnalysisStatus.FAILED);
				assertThat(recordAnalysis.getFailureReason())
					.contains(RecordAnalysisErrorCode.RECORD_ANALYSIS_PERSISTENCE_FAILED.getCode());
				assertThat(recordAnalysis.isFailureRetryable()).isTrue();
			});
	}

	private User createUser() {
		return userRepository.save(User.of(
			UUID.randomUUID() + "@test.com",
			"encoded-password",
			"테스터"
		));
	}

	private Activity createActivity(User user) {
		ActivityType activityType = activityTypeRepository.save(ActivityType.create(user, "프로젝트"));
		return activityRepository.save(Activity.create(
			user,
			activityType,
			"도트폴리오",
			"설명",
			LocalDate.now(),
			null,
			true
		));
	}

	private Template createTemplate(User user, boolean required) {
		Template template = Template.createCustom(user, "문제 해결", "설명");
		template.addQuestion(TemplateQuestion.create("질문", "설명", required, 1));
		return templateRepository.save(template);
	}

	private void connectTemplate(Activity activity, Template template) {
		activityTemplateRepository.save(ActivityTemplate.create(activity, template, 1));
	}

	@TestConfiguration
	static class RecordAnalysisServiceTestConfig {

		@Bean
		@Primary
		StubRecordAnalysisClient stubRecordAnalysisClient() {
			return new StubRecordAnalysisClient();
		}

		@Bean
		@Primary
		StubRecordEmbeddingWriter stubRecordEmbeddingWriter() {
			return new StubRecordEmbeddingWriter();
		}
	}

	static class StubRecordAnalysisClient implements RecordAnalysisClient {

		private RecordAnalysisResponse response;
		private RuntimeException failure;
		private Runnable beforeReturn;

		@Override
		public RecordAnalysisResponse analyze(RecordAnalysisRequest request) {
			if (failure != null) {
				throw failure;
			}
			if (beforeReturn != null) {
				beforeReturn.run();
			}
			return response;
		}

		private void reset() {
			this.response = null;
			this.failure = null;
			this.beforeReturn = null;
		}
	}

	static class StubRecordEmbeddingWriter implements RecordEmbeddingWriter {

		private UUID recordId;
		private String embeddingModel;
		private float[] embedding;
		private RuntimeException failure;

		@Override
		public void save(com.itcotato.dortfolio.domain.record.entity.Record record, String embeddingModel, float[] embedding) {
			if (failure != null) {
				throw failure;
			}
			this.recordId = record.getId();
			this.embeddingModel = embeddingModel;
			this.embedding = embedding;
		}

		private void reset() {
			this.recordId = null;
			this.embeddingModel = null;
			this.embedding = null;
			this.failure = null;
		}
	}
}
