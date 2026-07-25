package com.itcotato.dortfolio.domain.memo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.itcotato.dortfolio.domain.activity.entity.Activity;
import com.itcotato.dortfolio.domain.activity.entity.ActivityType;
import com.itcotato.dortfolio.domain.activity.repository.ActivityRepository;
import com.itcotato.dortfolio.domain.activity.repository.ActivityTypeRepository;
import com.itcotato.dortfolio.domain.memo.dto.req.MemoCreateRequest;
import com.itcotato.dortfolio.domain.memo.dto.req.MemoImagePresignedUrlRequest;
import com.itcotato.dortfolio.domain.memo.dto.req.MemoUpdateRequest;
import com.itcotato.dortfolio.domain.memo.dto.res.MemoImagePresignedUrlResponse;
import com.itcotato.dortfolio.domain.memo.dto.res.MemoResponse;
import com.itcotato.dortfolio.domain.memo.repository.MemoImageRepository;
import com.itcotato.dortfolio.domain.memo.repository.MemoRepository;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.ActivityErrorCode;
import com.itcotato.dortfolio.global.exception.types.MemoErrorCode;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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

	@BeforeEach
	void setUp() {
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
				List.of("https://s3/memo/a.png", "https://s3/memo/b.jpg")));

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
				new MemoCreateRequest(null, null, "내용", null, List.of("https://s3/memo/a.png")));

		memoService.deleteMemos(userId, List.of(memoId));

		assertThat(memoImageRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("만료되지 않은 메모는 자동 삭제 대상이 아니다")
	void doesNotDeleteUnexpiredMemos() {
		UUID userId = createUser().getId();
		memoService.createMemo(userId,
				new MemoCreateRequest(null, null, "내용", null, List.of("https://s3/memo/a.png")));

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
		MemoImagePresignedUrlResponse response =
				memoService.createMemoImagePresignedUrl(new MemoImagePresignedUrlRequest("사진.PNG"));

		assertThat(response.presignedUrl()).isNotBlank();
		assertThat(response.s3Key()).startsWith("memo/");
	}

	@Test
	@DisplayName("JPG, PNG 외 확장자는 Presigned URL 발급이 거부된다")
	void rejectPresignedUrlForDisallowedExtensions() {
		MemoImagePresignedUrlRequest request = new MemoImagePresignedUrlRequest("문서.pdf");

		assertThatThrownBy(() -> memoService.createMemoImagePresignedUrl(request))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode")
				.isEqualTo(MemoErrorCode.UNSUPPORTED_IMAGE_EXTENSION);
	}

	@Test
	@DisplayName("업로드된 활동 사진을 삭제할 수 있다")
	void deleteMemoImage() {
		UUID userId = createUser().getId();
		UUID memoId = memoService.createMemo(userId,
				new MemoCreateRequest(null, null, "내용", null, List.of("https://s3/memo/a.png")));
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
				new MemoCreateRequest(null, null, "내용", null, List.of("https://s3/memo/a.png")));
		UUID imageId = memoService.getMemo(ownerId, memoId).images().get(0).id();

		assertThatThrownBy(() -> memoService.deleteMemoImage(otherId, imageId))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode")
				.isEqualTo(MemoErrorCode.MEMO_IMAGE_NOT_FOUND);
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
}
