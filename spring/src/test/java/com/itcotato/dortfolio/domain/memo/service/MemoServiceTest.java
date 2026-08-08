package com.itcotato.dortfolio.domain.memo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.itcotato.dortfolio.domain.activity.entity.Activity;
import com.itcotato.dortfolio.domain.activity.entity.ActivityType;
import com.itcotato.dortfolio.domain.activity.repository.ActivityRepository;
import com.itcotato.dortfolio.domain.activity.repository.ActivityTypeRepository;
import com.itcotato.dortfolio.domain.memo.dto.req.MemoCreateRequest;
import com.itcotato.dortfolio.domain.memo.dto.req.MemoImagePresignedUrlRequest;
import com.itcotato.dortfolio.domain.memo.dto.req.MemoImageRequest;
import com.itcotato.dortfolio.domain.memo.dto.req.MemoUpdateRequest;
import com.itcotato.dortfolio.domain.memo.dto.res.MemoImagePresignedUrlResponse;
import com.itcotato.dortfolio.domain.memo.dto.res.MemoResponse;
import com.itcotato.dortfolio.domain.memo.repository.MemoImageRepository;
import com.itcotato.dortfolio.domain.memo.repository.MemoRepository;
import com.itcotato.dortfolio.domain.record.analysis.repository.RecordAnalysisRepository;
import com.itcotato.dortfolio.domain.record.entity.Record;
import com.itcotato.dortfolio.domain.record.entity.RecordMemo;
import com.itcotato.dortfolio.domain.record.repository.CompetencyTagRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordAnswerRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordCompetencyTagRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordMemoRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordRepository;
import com.itcotato.dortfolio.domain.template.entity.Template;
import com.itcotato.dortfolio.domain.template.repository.TemplateRepository;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.ActivityErrorCode;
import com.itcotato.dortfolio.global.exception.types.MemoErrorCode;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.transaction.support.TransactionTemplate;

@ActiveProfiles("test")
@SpringBootTest
class MemoServiceTest {

	@Autowired
	private MemoService memoService;

	@Autowired
	private MemoRepository memoRepository;

	@Autowired
	private MemoImageRepository memoImageRepository;

	@Autowired
	private ActivityRepository activityRepository;

	@Autowired
	private ActivityTypeRepository activityTypeRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RecordRepository recordRepository;

	@Autowired
	private RecordMemoRepository recordMemoRepository;

	@Autowired
	private TemplateRepository templateRepository;

	@Autowired
	private RecordAnalysisRepository recordAnalysisRepository;

	@Autowired
	private RecordCompetencyTagRepository recordCompetencyTagRepository;

	@Autowired
	private CompetencyTagRepository competencyTagRepository;

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
		memoImageRepository.deleteAll();
		memoRepository.deleteAll();
		activityRepository.deleteAll();
		activityTypeRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	@DisplayName("내용만으로 메모를 생성할 수 있다 (제목/활동 태그는 선택 입력)")
	void createMemoWithContentOnly() {
		UUID userId = createUser().getId();

		UUID memoId = memoService.createMemo(userId,
				new MemoCreateRequest(null, null, "내용만 있는 메모", null, null));

		MemoResponse response = memoService.getMemo(userId, memoId);
		assertThat(response.content()).isEqualTo("내용만 있는 메모");
		assertThat(response.title()).isNull();
		assertThat(response.activityId()).isNull();
		assertThat(response.images()).isEmpty();
		assertThat(response.imageCount()).isZero();
	}

	@Test
	@DisplayName("생성 직후 자동 삭제까지 남은 일수는 30일이다")
	void remainingDaysIsThirtyOnCreation() {
		UUID userId = createUser().getId();

		UUID memoId = memoService.createMemo(userId,
				new MemoCreateRequest(null, null, "내용", null, null));

		assertThat(memoService.getMemo(userId, memoId).remainingDaysUntilExpiration()).isEqualTo(30);
	}

	@Test
	@DisplayName("활동 태그와 이미지를 함께 저장할 수 있다")
	void createMemoWithActivityAndImages() {
		User user = createUser();
		Activity activity = createActivity(user);

		UUID memoId = memoService.createMemo(user.getId(), new MemoCreateRequest(
				activity.getId(), "제목", "내용", "BLUE",
				List.of(imageRequest(user.getId(), "a.png"), imageRequest(user.getId(), "b.jpg"))));

		MemoResponse response = memoService.getMemo(user.getId(), memoId);
		assertThat(response.activityId()).isEqualTo(activity.getId());
		assertThat(response.activityTitle()).isEqualTo("활동");
		assertThat(response.imageCount()).isEqualTo(2);
		assertThat(response.images()).extracting("sortOrder").containsExactly(0, 1);
	}

	@Test
	@DisplayName("삭제된 활동에는 메모를 연결할 수 없다")
	void cannotLinkDeletedActivity() {
		User user = createUser();
		Activity activity = createActivity(user);
		activity.markDeleted(30);
		activityRepository.save(activity);

		MemoCreateRequest request = new MemoCreateRequest(activity.getId(), null, "내용", null, null);

		assertThatThrownBy(() -> memoService.createMemo(user.getId(), request))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode")
				.isEqualTo(ActivityErrorCode.DELETED_ACTIVITY_NOT_LINKABLE);
	}

	@Test
	@DisplayName("존재하지 않는 활동에는 메모를 연결할 수 없다")
	void cannotLinkUnknownActivity() {
		UUID userId = createUser().getId();
		MemoCreateRequest request = new MemoCreateRequest(UUID.randomUUID(), null, "내용", null, null);

		assertThatThrownBy(() -> memoService.createMemo(userId, request))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode")
				.isEqualTo(ActivityErrorCode.ACTIVITY_NOT_FOUND);
	}

	@Test
	@DisplayName("메모 수정 시 제목/내용/색상만 변경되고 활동 태그는 유지된다")
	void updateMemoKeepsActivity() {
		User user = createUser();
		Activity activity = createActivity(user);
		UUID memoId = memoService.createMemo(user.getId(),
				new MemoCreateRequest(activity.getId(), "수정 전", "내용 전", "BLUE", null));

		memoService.updateMemo(user.getId(), memoId, new MemoUpdateRequest("수정 후", "내용 후", "GRAY"));

		MemoResponse response = memoService.getMemo(user.getId(), memoId);
		assertThat(response.title()).isEqualTo("수정 후");
		assertThat(response.content()).isEqualTo("내용 후");
		assertThat(response.color()).isEqualTo("GRAY");
		assertThat(response.activityId()).isEqualTo(activity.getId());
	}

	@Test
	@DisplayName("중요한 메모로 등록하거나 등록을 취소할 수 있다")
	void markImportantToggles() {
		UUID userId = createUser().getId();
		UUID memoId = memoService.createMemo(userId, new MemoCreateRequest(null, null, "내용", null, null));

		memoService.markImportant(userId, memoId, true);
		assertThat(memoService.getMemo(userId, memoId).isImportant()).isTrue();

		memoService.markImportant(userId, memoId, false);
		assertThat(memoService.getMemo(userId, memoId).isImportant()).isFalse();
	}

	@Test
	@DisplayName("활동 태그로 메모 목록을 필터링할 수 있다")
	void filterMemosByActivity() {
		User user = createUser();
		Activity activity = createActivity(user);
		memoService.createMemo(user.getId(), new MemoCreateRequest(activity.getId(), null, "태그 있음", null, null));
		memoService.createMemo(user.getId(), new MemoCreateRequest(null, null, "태그 없음", null, null));

		assertThat(memoService.getMemos(user.getId(), null)).hasSize(2);
		assertThat(memoService.getMemos(user.getId(), activity.getId()))
				.extracting(MemoResponse::content)
				.containsExactly("태그 있음");
	}

	@Test
	@DisplayName("메모를 1개 이상 한 번에 삭제할 수 있고 복구되지 않는다")
	void deleteMultipleMemos() {
		UUID userId = createUser().getId();
		UUID first = memoService.createMemo(userId, new MemoCreateRequest(null, null, "1", null, null));
		UUID second = memoService.createMemo(userId, new MemoCreateRequest(null, null, "2", null, null));

		memoService.deleteMemos(userId, List.of(first, second));

		assertThat(memoRepository.findAll()).isEmpty();
		assertThat(memoService.getMemos(userId, null)).isEmpty();
	}

	@Test
	@DisplayName("삭제 요청에 존재하지 않는 메모가 포함되면 아무것도 삭제되지 않는다")
	void deleteFailsWhenAnyMemoIsMissing() {
		UUID userId = createUser().getId();
		UUID memoId = memoService.createMemo(userId, new MemoCreateRequest(null, null, "내용", null, null));
		List<UUID> ids = List.of(memoId, UUID.randomUUID());

		assertThatThrownBy(() -> memoService.deleteMemos(userId, ids))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode")
				.isEqualTo(MemoErrorCode.MEMO_NOT_FOUND);

		assertThat(memoRepository.findAll()).hasSize(1);
	}

	@Test
	@DisplayName("메모 삭제 시 연결된 이미지도 함께 삭제된다")
	void deleteMemoAlsoDeletesImages() {
		UUID userId = createUser().getId();
		UUID memoId = memoService.createMemo(userId,
				new MemoCreateRequest(null, null, "내용", null, List.of(imageRequest(userId, "a.png"))));

		memoService.deleteMemos(userId, List.of(memoId));

		assertThat(memoImageRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("기록에 연결된 메모는 삭제할 수 없다")
	void cannotDeleteMemoLinkedToRecord() {
		User user = createUser();
		Activity activity = createActivity(user);
		UUID memoId = memoService.createMemo(user.getId(),
				new MemoCreateRequest(activity.getId(), null, "내용", null, null));
		linkMemoToRecord(user, activity, memoId);

		List<UUID> ids = List.of(memoId);

		assertThatThrownBy(() -> memoService.deleteMemos(user.getId(), ids))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode")
				.isEqualTo(MemoErrorCode.MEMO_LINKED_TO_RECORD);

		assertThat(memoRepository.findAll()).hasSize(1);
	}

	@Test
	@DisplayName("삭제 대상 중 하나라도 기록에 연결돼 있으면 전체 삭제가 실패한다")
	void deleteFailsWhenAnyMemoIsLinkedToRecord() {
		User user = createUser();
		Activity activity = createActivity(user);
		UUID linked = memoService.createMemo(user.getId(),
				new MemoCreateRequest(activity.getId(), null, "연결됨", null, null));
		UUID notLinked = memoService.createMemo(user.getId(),
				new MemoCreateRequest(activity.getId(), null, "연결 안 됨", null, null));
		linkMemoToRecord(user, activity, linked);

		List<UUID> ids = List.of(linked, notLinked);

		assertThatThrownBy(() -> memoService.deleteMemos(user.getId(), ids))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode")
				.isEqualTo(MemoErrorCode.MEMO_LINKED_TO_RECORD);

		assertThat(memoRepository.findAll()).hasSize(2);
	}

	@Test
	@DisplayName("기록에 연결된 메모는 만료되어도 자동 삭제되지 않는다")
	void expiredMemoLinkedToRecordIsNotDeleted() {
		User user = createUser();
		Activity activity = createActivity(user);
		UUID memoId = memoService.createMemo(user.getId(),
				new MemoCreateRequest(activity.getId(), null, "내용", null, null));
		linkMemoToRecord(user, activity, memoId);
		expireMemo(memoId);

		assertThat(memoService.deleteExpiredMemos()).isZero();
		assertThat(memoRepository.findAll()).hasSize(1);
	}

	@Test
	@DisplayName("기록에 연결되지 않은 만료 메모는 자동 삭제된다")
	void expiredMemoWithoutRecordIsDeleted() {
		UUID userId = createUser().getId();
		UUID memoId = memoService.createMemo(userId, new MemoCreateRequest(null, null, "내용", null, null));
		expireMemo(memoId);

		assertThat(memoService.deleteExpiredMemos()).isEqualTo(1);
		assertThat(memoRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("만료되지 않은 메모는 자동 삭제 대상이 아니다")
	void doesNotDeleteUnexpiredMemos() {
		UUID userId = createUser().getId();
		memoService.createMemo(userId,
				new MemoCreateRequest(null, null, "내용", null, List.of(imageRequest(userId, "a.png"))));

		assertThat(memoService.deleteExpiredMemos()).isZero();
		assertThat(memoRepository.findAll()).hasSize(1);
		assertThat(memoImageRepository.findAll()).hasSize(1);
	}

	@Test
	@DisplayName("다른 사용자의 메모는 조회할 수 없다")
	void cannotAccessOtherUsersMemo() {
		UUID ownerId = createUser().getId();
		UUID otherId = createUser().getId();
		UUID memoId = memoService.createMemo(ownerId, new MemoCreateRequest(null, null, "내용", null, null));

		assertThatThrownBy(() -> memoService.getMemo(otherId, memoId))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode")
				.isEqualTo(MemoErrorCode.MEMO_NOT_FOUND);
	}

	@Test
	@DisplayName("JPG, PNG 이미지에 대해 Presigned URL을 발급한다")
	void issuePresignedUrlForAllowedExtensions() {
		UUID userId = createUser().getId();

		MemoImagePresignedUrlResponse response =
				memoService.createMemoImagePresignedUrl(userId, new MemoImagePresignedUrlRequest("사진.PNG"));

		assertThat(response.presignedUrl()).isNotBlank();
		assertThat(response.s3Key()).startsWith("memo/" + userId + "/");
	}

	@Test
	@DisplayName("JPG, PNG 외 확장자는 Presigned URL 발급이 거부된다")
	void rejectPresignedUrlForDisallowedExtensions() {
		UUID userId = createUser().getId();
		MemoImagePresignedUrlRequest request = new MemoImagePresignedUrlRequest("문서.pdf");

		assertThatThrownBy(() -> memoService.createMemoImagePresignedUrl(userId, request))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode")
				.isEqualTo(MemoErrorCode.UNSUPPORTED_IMAGE_EXTENSION);
	}

	@Test
	@DisplayName("업로드된 활동 사진을 삭제할 수 있다")
	void deleteMemoImage() {
		UUID userId = createUser().getId();
		UUID memoId = memoService.createMemo(userId,
				new MemoCreateRequest(null, null, "내용", null, List.of(imageRequest(userId, "a.png"))));
		UUID imageId = memoService.getMemo(userId, memoId).images().get(0).id();

		memoService.deleteMemoImage(userId, imageId);

		assertThat(memoService.getMemo(userId, memoId).images()).isEmpty();
	}

	@Test
	@DisplayName("다른 사용자의 활동 사진은 삭제할 수 없다")
	void cannotDeleteOtherUsersImage() {
		UUID ownerId = createUser().getId();
		UUID otherId = createUser().getId();
		UUID memoId = memoService.createMemo(ownerId,
				new MemoCreateRequest(null, null, "내용", null, List.of(imageRequest(ownerId, "a.png"))));
		UUID imageId = memoService.getMemo(ownerId, memoId).images().get(0).id();

		assertThatThrownBy(() -> memoService.deleteMemoImage(otherId, imageId))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode")
				.isEqualTo(MemoErrorCode.MEMO_IMAGE_NOT_FOUND);
	}

	// 메모를 기록에 연결한다 (record_memos.memo_id 필수 FK 상황 재현)
	private void linkMemoToRecord(User user, Activity activity, UUID memoId) {
		Template template = templateRepository.save(Template.createCustom(user, "템플릿", null));
		Record record = recordRepository.save(Record.builder()
				.user(user)
				.activity(activity)
				.template(template)
				.title("기록")
				.build());

		recordMemoRepository.save(RecordMemo.builder()
				.record(record)
				.memo(memoRepository.findById(memoId).orElseThrow())
				.sortOrder(1)
				.isCollapsed(false)
				.build());
	}

	// expiresAt은 생성 시점에만 세팅되므로 만료 상황은 벌크 업데이트로 재현한다
	private void expireMemo(UUID memoId) {
		transactionTemplate.executeWithoutResult(status ->
				entityManager.createQuery("update Memo m set m.expiresAt = :expiredAt where m.id = :id")
						.setParameter("expiredAt", LocalDateTime.now().minusDays(1))
						.setParameter("id", memoId)
						.executeUpdate());
	}

	private User createUser() {
		return userRepository.save(User.of(
				UUID.randomUUID() + "@test.com",
				"encoded-password",
				"테스터"
		));
	}

	private Activity createActivity(User user) {
		ActivityType activityType = activityTypeRepository.save(ActivityType.create(user, "동아리"));
		return activityRepository.save(Activity.create(
				user,
				activityType,
				"활동",
				"설명",
				LocalDate.now().minusDays(10),
				LocalDate.now(),
				false
		));
	}

	@Test
	@DisplayName("다른 사용자의 s3Key로는 메모 이미지를 저장할 수 없다")
	void cannotUseOtherUsersImageKey() {
		UUID ownerId = createUser().getId();
		UUID attackerId = createUser().getId();

		MemoCreateRequest request = new MemoCreateRequest(
				null, null, "내용", null, List.of(imageRequest(ownerId, "a.png")));

		assertThatThrownBy(() -> memoService.createMemo(attackerId, request))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode")
				.isEqualTo(MemoErrorCode.INVALID_IMAGE_KEY);
	}

	@Test
	@DisplayName("경로가 조작된 s3Key는 거부한다")
	void rejectsTamperedImageKey() {
		UUID userId = createUser().getId();

		MemoCreateRequest request = new MemoCreateRequest(
				null, null, "내용", null,
				List.of(new MemoImageRequest("https://s3/memo/a.png", "memo/a.png")));

		assertThatThrownBy(() -> memoService.createMemo(userId, request))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode")
				.isEqualTo(MemoErrorCode.INVALID_IMAGE_KEY);
	}

	// 업로드 키에는 소유자 ID가 들어간다. 남의 키를 쓰면 저장 단계에서 막힌다
	private MemoImageRequest imageRequest(UUID userId, String fileName) {
		String s3Key = "memo/" + userId + "/" + fileName;
		return new MemoImageRequest("https://s3/" + s3Key, s3Key);
	}
}
