package com.itcotato.dortfolio.domain.record.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.itcotato.dortfolio.domain.activity.entity.Activity;
import com.itcotato.dortfolio.domain.activity.entity.ActivityType;
import com.itcotato.dortfolio.domain.activity.repository.ActivityRepository;
import com.itcotato.dortfolio.domain.activity.repository.ActivityTypeRepository;
import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisRequest;
import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisResponse;
import com.itcotato.dortfolio.domain.record.analysis.dto.StrengthMatchCandidate;
import com.itcotato.dortfolio.domain.record.analysis.entity.AiAnalysisStatus;
import com.itcotato.dortfolio.domain.record.analysis.exception.RecordAnalysisErrorCode;
import com.itcotato.dortfolio.domain.record.analysis.repository.RecordAnalysisRepository;
import com.itcotato.dortfolio.domain.record.analysis.repository.StrengthMatchCandidateQuery;
import com.itcotato.dortfolio.domain.record.dto.req.RecordAnswerRequest;
import com.itcotato.dortfolio.domain.record.dto.req.RecordCreateRequest;
import com.itcotato.dortfolio.domain.record.dto.req.RecordUpdateRequest;
import com.itcotato.dortfolio.domain.record.dto.res.RecordResponse;
import com.itcotato.dortfolio.domain.record.entity.StrengthTag;
import com.itcotato.dortfolio.domain.record.entity.RecordStatus;
import com.itcotato.dortfolio.domain.record.repository.StrengthTagRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordAnswerRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordStrengthTagRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordEmbeddingRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordMemoRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordRepository;
import com.itcotato.dortfolio.domain.record.service.RecordService;
import com.itcotato.dortfolio.domain.template.entity.Template;
import com.itcotato.dortfolio.domain.template.entity.TemplateQuestion;
import com.itcotato.dortfolio.domain.template.repository.TemplateRepository;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import com.itcotato.dortfolio.global.ai.embedding.dto.EmbeddingRequest;
import com.itcotato.dortfolio.global.ai.embedding.dto.EmbeddingResponse;
import com.itcotato.dortfolio.global.ai.embedding.service.EmbeddingClient;
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
	private StubEmbeddingClient stubEmbeddingClient;

	@Autowired
	private StubRecordEmbeddingTextBuilder stubRecordEmbeddingTextBuilder;

	@Autowired
	private StubStrengthMatchCandidateQuery stubStrengthMatchCandidateQuery;

	@Autowired
	private RecordAnalysisRepository recordAnalysisRepository;

	@Autowired
	private RecordEmbeddingRepository recordEmbeddingRepository;

	@Autowired
	private RecordStrengthTagRepository recordStrengthTagRepository;

	@Autowired
	private StrengthTagRepository strengthTagRepository;

	@Autowired
	private RecordMemoRepository recordMemoRepository;

	@Autowired
	private RecordAnswerRepository recordAnswerRepository;

	@Autowired
	private RecordRepository recordRepository;

	@Autowired
	private TemplateRepository templateRepository;

	@Autowired
	private ActivityRepository activityRepository;

	@Autowired
	private ActivityTypeRepository activityTypeRepository;

	@Autowired
	private UserRepository userRepository;

	private List<StrengthTag> strengthCandidates;

	@BeforeEach
	void setUp() {
		stubRecordAnalysisClient.reset();
		stubRecordEmbeddingWriter.reset();
		stubEmbeddingClient.reset();
		stubRecordEmbeddingTextBuilder.reset();
		stubStrengthMatchCandidateQuery.reset();
		recordAnalysisRepository.deleteAll();
		recordEmbeddingRepository.deleteAll();
		recordStrengthTagRepository.deleteAll();
		strengthTagRepository.deleteAll();
		recordMemoRepository.deleteAll();
		recordAnswerRepository.deleteAll();
		recordRepository.deleteAll();
		templateRepository.deleteAll();
		activityRepository.deleteAll();
		activityTypeRepository.deleteAll();
		userRepository.deleteAll();
		strengthCandidates = null;
	}

	@Test
	void analyzeCompletedRecordSavesAnalysisEmbeddingAndStrengthTags() {
		User user = createUser();
		Activity activity = createActivity(user);
		Template template = createTemplate(user, true);
		StrengthTag strengthTag = strengthCandidates.get(0);
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
			List.of(strengthTag.getId())
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
		assertThat(stubRecordEmbeddingWriter.embedding).hasSize(3072);
		assertThat(recordStrengthTagRepository.findAllByRecord_Id(record.id()))
			.singleElement()
			.satisfies(tag -> {
				assertThat(tag.getStrengthTag().getId()).isEqualTo(strengthTag.getId());
				assertThat(tag.getCosineSimilarity()).isEqualTo(0.9f);
			});
	}

	@Test
	void analyzeStoresFailedStatusWhenClientFails() {
		User user = createUser();
		Activity activity = createActivity(user);
		Template template = createTemplate(user, false);
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
		assertThat(recordAnalysisRepository.findRetryableRecordIds()).contains(record.id());
		assertThat(recordEmbeddingRepository.countByRecord_Id(record.id())).isZero();
		assertThat(recordStrengthTagRepository.findAllByRecord_Id(record.id())).isEmpty();
	}

	@Test
	void analyzeStoresRetryableFailureWhenUnexpectedExecutionErrorOccurs() {
		User user = createUser();
		Activity activity = createActivity(user);
		Template template = createTemplate(user, false);
		RecordResponse record = recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(), template.getId(), "실행 오류 기록", List.of(), List.of(), RecordStatus.COMPLETED
		));
		stubRecordEmbeddingTextBuilder.failure = new IllegalStateException("unexpected execution failure");

		recordAnalysisService.analyze(record.id());

		assertThat(recordAnalysisRepository.findByRecord_Id(record.id()).orElseThrow())
			.satisfies(recordAnalysis -> {
				assertThat(recordAnalysis.getAiAnalysisStatus()).isEqualTo(AiAnalysisStatus.FAILED);
				assertThat(recordAnalysis.getFailureReason())
					.contains(RecordAnalysisErrorCode.RECORD_ANALYSIS_PERSISTENCE_FAILED.getCode());
				assertThat(recordAnalysis.isFailureRetryable()).isTrue();
			});
	}

	@Test
	void analyzeRejectsEmbeddingWithoutMagnitude() {
		User user = createUser();
		Activity activity = createActivity(user);
		Template template = createTemplate(user, false);
		RecordResponse record = recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(), template.getId(), "영 벡터 기록", List.of(), List.of(), RecordStatus.COMPLETED
		));
		stubEmbeddingClient.embedding = new float[3072];

		recordAnalysisService.analyze(record.id());

		assertThat(recordAnalysisRepository.findByRecord_Id(record.id()).orElseThrow())
			.satisfies(recordAnalysis -> {
				assertThat(recordAnalysis.getAiAnalysisStatus()).isEqualTo(AiAnalysisStatus.FAILED);
				assertThat(recordAnalysis.getFailureReason())
					.contains(RecordAnalysisErrorCode.RECORD_ANALYSIS_INVALID_RESPONSE.getCode());
			});
	}

	@Test
	void analyzeStoresRejectedStatusWhenAiServiceReturnsClientError() {
		User user = createUser();
		Activity activity = createActivity(user);
		Template template = createTemplate(user, false);
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
		assertThat(recordAnalysisRepository.findRetryableRecordIds()).doesNotContain(record.id());
	}

	@Test
	void analyzeStoresRetryableFailureWhenAiServiceReturnsServerError() {
		User user = createUser();
		Activity activity = createActivity(user);
		Template template = createTemplate(user, false);
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
		assertThat(recordAnalysisRepository.findRetryableRecordIds()).contains(record.id());
	}

	@Test
	void failedReanalysisKeepsPreviousAnalysisDataAndStoresRetryMetadata() {
		User user = createUser();
		Activity activity = createActivity(user);
		Template template = createTemplate(user, false);
		StrengthTag strengthTag = strengthCandidates.get(0);
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
			List.of(strengthTag.getId())
		);
		recordAnalysisService.analyze(record.id());
		stubRecordAnalysisClient.reset();
		stubRecordAnalysisClient.failure = new IllegalStateException("AI service unavailable");

		recordAnalysisService.analyze(record.id());

		assertThat(recordAnalysisRepository.findByRecord_Id(record.id()).orElseThrow())
			.satisfies(recordAnalysis -> {
				assertThat(recordAnalysis.getAiAnalysisStatus()).isEqualTo(AiAnalysisStatus.COMPLETED);
				assertThat(recordAnalysis.getSummary()).isEqualTo("기존 요약");
				assertThat(recordAnalysis.getEvidenceSnippets()).contains("기존 근거");
				assertThat(recordAnalysis.isLastAttemptFailed()).isTrue();
				assertThat(recordAnalysis.isLastFailureRetryable()).isTrue();
				assertThat(recordAnalysis.getLastFailureReason())
					.contains(RecordAnalysisErrorCode.RECORD_ANALYSIS_AI_SERVICE_FAILED.getCode());
			});
		assertThat(recordAnalysisRepository.findRetryableRecordIds()).contains(record.id());
		assertThat(recordStrengthTagRepository.findAllByRecord_Id(record.id())).hasSize(1);
	}

	@Test
	void successfulReanalysisCanSelectTheSameStrengthTagAgain() {
		User user = createUser();
		Activity activity = createActivity(user);
		Template template = createTemplate(user, false);
		StrengthTag strengthTag = strengthCandidates.get(0);
		RecordResponse record = recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(),
			template.getId(),
			"재분석 기록",
			List.of(),
			List.of(),
			RecordStatus.COMPLETED
		));
		stubRecordAnalysisClient.response = new RecordAnalysisResponse(
			"첫 번째 요약",
			List.of("첫 번째 근거"),
			List.of(strengthTag.getId())
		);
		recordAnalysisService.analyze(record.id());
		stubRecordAnalysisClient.response = new RecordAnalysisResponse(
			"두 번째 요약",
			List.of("두 번째 근거"),
			List.of(strengthTag.getId())
		);

		recordAnalysisService.analyze(record.id());

		assertThat(recordAnalysisRepository.findByRecord_Id(record.id()).orElseThrow())
			.satisfies(recordAnalysis -> {
				assertThat(recordAnalysis.getAiAnalysisStatus()).isEqualTo(AiAnalysisStatus.COMPLETED);
				assertThat(recordAnalysis.getSummary()).isEqualTo("두 번째 요약");
			});
		assertThat(recordStrengthTagRepository.findAllByRecord_Id(record.id()))
			.singleElement()
			.satisfies(recordTag ->
				assertThat(recordTag.getStrengthTag().getId()).isEqualTo(strengthTag.getId())
			);
	}

	@Test
	void analyzeStoresFailedStatusWhenResponseHasDuplicateStrengthTags() {
		User user = createUser();
		Activity activity = createActivity(user);
		Template template = createTemplate(user, false);
		StrengthTag strengthTag = strengthCandidates.get(0);
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
				strengthTag.getId(),
				strengthTag.getId()
			)
		);

		recordAnalysisService.analyze(record.id());

		assertThat(recordAnalysisRepository.findByRecord_Id(record.id()).orElseThrow())
			.satisfies(recordAnalysis -> {
				assertThat(recordAnalysis.getAiAnalysisStatus()).isEqualTo(AiAnalysisStatus.FAILED);
				assertThat(recordAnalysis.getFailureReason())
					.contains(RecordAnalysisErrorCode.RECORD_ANALYSIS_INVALID_RESPONSE.getCode());
			});
		assertThat(recordStrengthTagRepository.findAllByRecord_Id(record.id())).isEmpty();
	}

	@Test
	void analyzeRejectsMoreStrengthTagsThanConfiguredMaximum() {
		User user = createUser();
		Activity activity = createActivity(user);
		Template template = createTemplate(user, false);
		RecordResponse record = recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(),
			template.getId(),
			"강점 개수 초과 기록",
			List.of(),
			List.of(),
			RecordStatus.COMPLETED
		));
		stubRecordAnalysisClient.response = new RecordAnalysisResponse(
			"요약",
			List.of("근거"),
			strengthCandidates.stream().limit(3).map(StrengthTag::getId).toList()
		);

		recordAnalysisService.analyze(record.id());

		assertThat(recordAnalysisRepository.findByRecord_Id(record.id()).orElseThrow())
			.satisfies(recordAnalysis -> {
				assertThat(recordAnalysis.getAiAnalysisStatus()).isEqualTo(AiAnalysisStatus.FAILED);
				assertThat(recordAnalysis.getFailureReason())
					.contains(RecordAnalysisErrorCode.RECORD_ANALYSIS_INVALID_RESPONSE.getCode());
			});
		assertThat(recordStrengthTagRepository.findAllByRecord_Id(record.id())).isEmpty();
	}

	@Test
	void analyzeDoesNotSaveResultWhenRecordIsDeletedBeforePersistingResponse() {
		User user = createUser();
		Activity activity = createActivity(user);
		Template template = createTemplate(user, false);
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
			List.of()
		);
		stubRecordAnalysisClient.beforeReturn = () -> recordService.deleteRecord(user.getId(), record.id());

		recordAnalysisService.analyze(record.id());

		assertThat(recordAnalysisRepository.findByRecord_Id(record.id())).isEmpty();
		assertThat(recordStrengthTagRepository.findAllByRecord_Id(record.id())).isEmpty();
		assertThat(stubRecordEmbeddingWriter.recordId).isNull();
	}

	@Test
	void analyzeKeepsPreviousAnalysisWhenRecordIsDeletedBeforePersistingReanalysisResponse() {
		User user = createUser();
		Activity activity = createActivity(user);
		Template template = createTemplate(user, false);
		StrengthTag strengthTag = strengthCandidates.get(0);
		RecordResponse record = recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(),
			template.getId(),
			"삭제 중 재분석 기록",
			List.of(),
			List.of(),
			RecordStatus.COMPLETED
		));
		stubRecordAnalysisClient.response = new RecordAnalysisResponse(
			"기존 요약",
			List.of("기존 근거"),
			List.of(strengthTag.getId())
		);
		recordAnalysisService.analyze(record.id());

		stubRecordEmbeddingWriter.reset();
		stubRecordAnalysisClient.response = new RecordAnalysisResponse(
			"삭제 후 도착한 요약",
			List.of("삭제 후 도착한 근거"),
			List.of()
		);
		stubRecordAnalysisClient.beforeReturn = () -> recordService.deleteRecord(user.getId(), record.id());

		recordAnalysisService.analyze(record.id());

		assertThat(recordAnalysisRepository.findByRecord_Id(record.id()).orElseThrow())
			.satisfies(recordAnalysis -> {
				assertThat(recordAnalysis.getAiAnalysisStatus()).isEqualTo(AiAnalysisStatus.COMPLETED);
				assertThat(recordAnalysis.getSummary()).isEqualTo("기존 요약");
				assertThat(recordAnalysis.getEvidenceSnippets()).contains("기존 근거");
			});
		assertThat(recordStrengthTagRepository.findAllByRecord_Id(record.id())).hasSize(1);
		assertThat(stubRecordEmbeddingWriter.recordId).isNull();
	}

	@Test
	void analyzeDoesNotSaveStaleResultWhenRecordChangesBeforePersistingResponse() {
		User user = createUser();
		Activity activity = createActivity(user);
		Template template = createTemplate(user, false);
		RecordResponse record = recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(),
			template.getId(),
			"수정 전 기록",
			List.of(),
			List.of(),
			RecordStatus.COMPLETED
		));
		stubRecordAnalysisClient.response = new RecordAnalysisResponse(
			"오래된 요약",
			List.of("오래된 근거"),
			List.of()
		);
		stubRecordAnalysisClient.beforeReturn = () -> recordService.updateRecord(user.getId(), record.id(), new RecordUpdateRequest(
			"수정 후 기록",
			List.of(),
			List.of(),
			RecordStatus.COMPLETED
		));

		recordAnalysisService.analyze(record.id());

		assertThat(recordAnalysisRepository.findByRecord_Id(record.id())).isEmpty();
		assertThat(recordStrengthTagRepository.findAllByRecord_Id(record.id())).isEmpty();
		assertThat(stubRecordEmbeddingWriter.recordId).isNull();
	}

	@Test
	void analyzeStoresFailedStatusWhenEmbeddingPersistenceFails() {
		User user = createUser();
		Activity activity = createActivity(user);
		Template template = createTemplate(user, false);
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
			List.of()
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

	@Test
	void analyzePassesAllConfiguredStrengthTags() {
		User user = createUser();
		Activity activity = createActivity(user);
		Template template = createTemplate(user, false);
		RecordResponse record = recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(), template.getId(), "후보 순서 기록", List.of(), List.of(), RecordStatus.COMPLETED
		));
		stubRecordAnalysisClient.response = new RecordAnalysisResponse("요약", List.of("근거"), List.of());

		recordAnalysisService.analyze(record.id());

		assertThat(stubRecordAnalysisClient.lastRequest.strengthTagCandidates())
			.extracting(RecordAnalysisRequest.StrengthTagCandidatePayload::id)
			.containsExactlyElementsOf(strengthCandidates.stream().map(StrengthTag::getId).toList());
	}

	@Test
	void analyzeRejectsStrengthOutsideConfiguredCandidates() {
		User user = createUser();
		Activity activity = createActivity(user);
		Template template = createTemplate(user, false);
		UUID outsideCandidateId = UUID.randomUUID();
		RecordResponse record = recordService.createRecord(user.getId(), new RecordCreateRequest(
			activity.getId(), template.getId(), "후보 검증 기록", List.of(), List.of(), RecordStatus.COMPLETED
		));
		stubRecordAnalysisClient.response = new RecordAnalysisResponse(
			"요약",
			List.of("근거"),
			List.of(outsideCandidateId)
		);

		recordAnalysisService.analyze(record.id());

		assertThat(recordAnalysisRepository.findByRecord_Id(record.id()).orElseThrow())
			.satisfies(recordAnalysis -> {
				assertThat(recordAnalysis.getAiAnalysisStatus()).isEqualTo(AiAnalysisStatus.FAILED);
				assertThat(recordAnalysis.getFailureReason())
					.contains(RecordAnalysisErrorCode.RECORD_ANALYSIS_INVALID_RESPONSE.getCode());
			});
		assertThat(recordStrengthTagRepository.findAllByRecord_Id(record.id())).isEmpty();
	}

	private User createUser() {
		User user = userRepository.save(User.of(
			UUID.randomUUID() + "@test.com",
			"encoded-password",
			"테스터"
		));
		strengthCandidates = java.util.stream.IntStream.rangeClosed(1, 5)
			.mapToObj(index -> strengthTagRepository.save(StrengthTag.create(
				"TEST_STRENGTH_" + index + "_" + UUID.randomUUID().toString().substring(0, 8),
				"테스트 강점 " + index,
				"테스트 강점 설명 " + index,
				"테스트 판단 기준 " + index,
				"테스트 적합 예시 " + index,
				"테스트 부적합 예시 " + index
			)))
			.toList();
		stubStrengthMatchCandidateQuery.candidates = strengthCandidates.stream()
			.map(tag -> new StrengthMatchCandidate(
				tag.getId(),
				tag.getName(),
				tag.getDescription(),
				tag.getEvaluationCriteria(),
				tag.getPositiveExample(),
				tag.getNegativeExample(),
				0.9f
			))
			.toList();
		return user;
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
		template.initializeQuestions(List.of(TemplateQuestion.create("질문", "설명", required, 1)));
		return templateRepository.save(template);
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

		@Bean
		@Primary
		StubEmbeddingClient stubEmbeddingClient() {
			return new StubEmbeddingClient();
		}

		@Bean
		@Primary
		StubRecordEmbeddingTextBuilder stubRecordEmbeddingTextBuilder() {
			return new StubRecordEmbeddingTextBuilder();
		}

		@Bean
		@Primary
		StubStrengthMatchCandidateQuery stubStrengthMatchCandidateQuery() {
			return new StubStrengthMatchCandidateQuery();
		}
	}

	static class StubEmbeddingClient implements EmbeddingClient {

		private RuntimeException failure;
		private float[] embedding = validEmbedding();

		@Override
		public EmbeddingResponse embed(EmbeddingRequest request) {
			if (failure != null) {
				throw failure;
			}
			return new EmbeddingResponse("test-embedding", embedding);
		}

		private void reset() {
			this.failure = null;
			this.embedding = validEmbedding();
		}

		private static float[] validEmbedding() {
			float[] embedding = new float[3072];
			embedding[0] = 1.0f;
			return embedding;
		}
	}

	static class StubRecordEmbeddingTextBuilder extends RecordEmbeddingTextBuilder {

		private RuntimeException failure;

		@Override
		public String build(RecordAnalysisRequest request) {
			if (failure != null) {
				throw failure;
			}
			return super.build(request);
		}

		private void reset() {
			this.failure = null;
		}
	}

	static class StubStrengthMatchCandidateQuery implements StrengthMatchCandidateQuery {

		private List<StrengthMatchCandidate> candidates = List.of();

		@Override
		public List<StrengthMatchCandidate> findTopCandidates(
			String embeddingModel,
			float[] recordEmbedding,
			int limit,
			double minimumSimilarity
		) {
			return candidates.stream().limit(limit).toList();
		}

		private void reset() {
			this.candidates = List.of();
		}
	}

	static class StubRecordAnalysisClient implements RecordAnalysisClient {

		private RecordAnalysisResponse response;
		private RuntimeException failure;
		private Runnable beforeReturn;
		private RecordAnalysisRequest lastRequest;

		@Override
		public RecordAnalysisResponse analyze(RecordAnalysisRequest request) {
			this.lastRequest = request;
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
			this.lastRequest = null;
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
