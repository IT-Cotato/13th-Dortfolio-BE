package com.itcotato.dortfolio.domain.memo.service;

import com.itcotato.dortfolio.domain.activity.entity.Activity;
import com.itcotato.dortfolio.domain.activity.repository.ActivityRepository;
import com.itcotato.dortfolio.domain.memo.dto.req.MemoCreateRequest;
import com.itcotato.dortfolio.domain.memo.dto.req.MemoImagePresignedUrlRequest;
import com.itcotato.dortfolio.domain.memo.dto.req.MemoImageRequest;
import com.itcotato.dortfolio.domain.memo.dto.req.MemoUpdateRequest;
import com.itcotato.dortfolio.domain.memo.dto.res.MemoImagePresignedUrlResponse;
import com.itcotato.dortfolio.domain.memo.dto.res.MemoResponse;
import com.itcotato.dortfolio.domain.memo.entity.Memo;
import com.itcotato.dortfolio.domain.memo.entity.MemoImage;
import com.itcotato.dortfolio.domain.memo.repository.MemoImageRepository;
import com.itcotato.dortfolio.domain.memo.repository.MemoRepository;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import com.itcotato.dortfolio.global.aws.S3Provider;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.ActivityErrorCode;
import com.itcotato.dortfolio.global.exception.types.GlobalErrorCode;
import com.itcotato.dortfolio.global.exception.types.MemoErrorCode;
import com.itcotato.dortfolio.global.exception.types.UserErrorCode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemoService {

    private static final int DEFAULT_SORT_ORDER = 0;
    private static final String IMAGE_PREFIX = "memo";
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    private final MemoRepository memoRepository;
    private final MemoImageRepository memoImageRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final S3Provider s3Provider;

    @Transactional
    public UUID createMemo(UUID userId, MemoCreateRequest request) {
        User user = getUserOrThrow(userId);
        Activity activity = resolveActivityOrNull(request.activityId(), userId);

        Memo memo = memoRepository.save(Memo.create(
                user,
                activity,
                request.title(),
                request.content(),
                request.color(),
                DEFAULT_SORT_ORDER
        ));

        saveMemoImages(memo, request.images());

        return memo.getId();
    }

    // 기능명세서 3.4: activityId가 null이면 전체보기, 있으면 해당 활동 태그로 필터링
    public List<MemoResponse> getMemos(UUID userId, UUID activityId) {
        List<Memo> memos = (activityId == null)
                ? memoRepository.findAllByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
                : memoRepository.findAllByUser_IdAndActivity_IdAndDeletedAtIsNullOrderByCreatedAtDesc(userId, activityId);

        if (memos.isEmpty()) {
            return List.of();
        }

        // N+1 방지: 전체 메모의 이미지를 한 번에 조회 후 메모별로 그룹핑
        List<UUID> memoIds = memos.stream().map(Memo::getId).toList();
        Map<UUID, List<MemoImage>> imagesByMemoId =
                memoImageRepository.findAllByMemo_IdInOrderBySortOrderAsc(memoIds).stream()
                        .collect(Collectors.groupingBy(memoImage -> memoImage.getMemo().getId()));

        return memos.stream()
                .map(memo -> MemoResponse.from(
                        memo,
                        imagesByMemoId.getOrDefault(memo.getId(), List.of()),
                        s3Provider::generateDownloadUrl))
                .toList();
    }

    public MemoResponse getMemo(UUID userId, UUID memoId) {
        Memo memo = getMemoOrThrow(memoId, userId);
        return MemoResponse.from(
                memo,
                memoImageRepository.findAllByMemo_IdOrderBySortOrderAsc(memoId),
                s3Provider::generateDownloadUrl);
    }

    @Transactional
    public void updateMemo(UUID userId, UUID memoId, MemoUpdateRequest request) {
        Memo memo = getMemoOrThrow(memoId, userId);

        memo.update(request.title(), request.content(), request.color());
    }

    @Transactional
    public void markImportant(UUID userId, UUID memoId, boolean important) {
        getMemoOrThrow(memoId, userId).markImportant(important);
    }

    // 기능명세서 3.2.3.1.1 / 3.3.3: 단건이든 다건(1~n개)이든 동일하게 처리, 복구 불가 -> 하드 삭제
    @Transactional
    public void deleteMemos(UUID userId, List<UUID> memoIds) {
        if (memoIds == null || memoIds.isEmpty()) {
            throw new CustomException(GlobalErrorCode.INVALID_INPUT_VALUE);
        }

        Set<UUID> requestedIds = new HashSet<>(memoIds);
        List<Memo> memos = memoRepository.findAllByIdInAndUser_Id(memoIds, userId);

        // 요청한 메모 중 하나라도 없거나 내 소유가 아니면 전체 삭제를 막는다
        if (memos.size() != requestedIds.size()) {
            throw new CustomException(MemoErrorCode.MEMO_NOT_FOUND);
        }

        // 기록에 연결된 메모는 기록 작성의 근거로 쓰였으므로 삭제를 막는다 (하나라도 있으면 전체 실패)
        if (!memoRepository.findIdsLinkedToRecords(memoIds).isEmpty()) {
            throw new CustomException(MemoErrorCode.MEMO_LINKED_TO_RECORD);
        }

        deleteMemosWithImages(memos);
    }

    /* 기능명세서 3. 메모하기: 생성 30일 후 자동 삭제 (스케줄러에서 호출) */
    @Transactional
    public int deleteExpiredMemos() {
        List<Memo> expiredMemos = memoRepository.findAllByExpiresAtBefore(LocalDateTime.now());

        if (expiredMemos.isEmpty()) {
            return 0;
        }

        // 기록에 연결된 메모는 삭제 정책상 만료되어도 삭제하지 않는다 (FK 제약 위반 방지)
        Set<UUID> linkedMemoIds = new HashSet<>(
                memoRepository.findIdsLinkedToRecords(expiredMemos.stream().map(Memo::getId).toList()));

        List<Memo> deletableMemos = expiredMemos.stream()
                .filter(memo -> !linkedMemoIds.contains(memo.getId()))
                .toList();

        deleteMemosWithImages(deletableMemos);
        return deletableMemos.size();
    }

    // 메모를 하드 삭제하기 전에 FK로 연결된 이미지를 먼저 정리한다
    private void deleteMemosWithImages(List<Memo> memos) {
        if (memos.isEmpty()) {
            return;
        }

        List<UUID> memoIds = memos.stream().map(Memo::getId).toList();
        List<MemoImage> memoImages = memoImageRepository.findAllByMemo_IdInOrderBySortOrderAsc(memoIds);

        // 이미지 삭제를 먼저 DB에 반영해야 memo_images의 FK 제약에 걸리지 않는다
        memoImageRepository.deleteAllByMemo_IdIn(memoIds);
        memoImageRepository.flush();

        memoRepository.deleteAll(memos);

        memoImages.forEach(memoImage -> s3Provider.deleteObject(memoImage.getS3Key()));
    }

    /* 활동 사진 업로드 Presigned URL 발급 (기능명세서 3.1.3) */
    public MemoImagePresignedUrlResponse createMemoImagePresignedUrl(MemoImagePresignedUrlRequest request) {
        validateImageExtension(request.fileName());

        S3Provider.PresignedUrlResponse result = s3Provider.generatePresignedUrl(IMAGE_PREFIX, request.fileName());
        return new MemoImagePresignedUrlResponse(result.presignedUrl(), result.s3Key());
    }

    /* 업로드된 활동 사진 삭제 (기능명세서 3.1.4): 메타데이터와 S3 객체를 함께 제거 */
    @Transactional
    public void deleteMemoImage(UUID userId, UUID imageId) {
        MemoImage memoImage = memoImageRepository.findByIdAndMemo_User_Id(imageId, userId)
                .orElseThrow(() -> new CustomException(MemoErrorCode.MEMO_IMAGE_NOT_FOUND));

        memoImageRepository.delete(memoImage);
        s3Provider.deleteObject(memoImage.getS3Key());
    }

    private void saveMemoImages(Memo memo, List<MemoImageRequest> images) {
        if (images == null || images.isEmpty()) {
            return;
        }

        List<MemoImage> memoImages = IntStream.range(0, images.size())
                .mapToObj(index -> {
                    MemoImageRequest image = images.get(index);
                    return MemoImage.create(memo, image.imageUrl(), image.s3Key(), index);
                })
                .toList();

        memoImageRepository.saveAll(memoImages);
    }

    private void validateImageExtension(String fileName) {
        int lastIndexOfDot = fileName.lastIndexOf(".");

        if (lastIndexOfDot == -1) {
            throw new CustomException(MemoErrorCode.UNSUPPORTED_IMAGE_EXTENSION);
        }

        String extension = fileName.substring(lastIndexOfDot + 1).toLowerCase(Locale.ROOT);

        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new CustomException(MemoErrorCode.UNSUPPORTED_IMAGE_EXTENSION);
        }
    }

    private Memo getMemoOrThrow(UUID memoId, UUID userId) {
        return memoRepository.findByIdAndUser_Id(memoId, userId)
                .orElseThrow(() -> new CustomException(MemoErrorCode.MEMO_NOT_FOUND));
    }

    private Activity resolveActivityOrNull(UUID activityId, UUID userId) {
        if (activityId == null) {
            return null;
        }
        Activity activity = activityRepository.findByIdAndUser_Id(activityId, userId)
                .orElseThrow(() -> new CustomException(ActivityErrorCode.ACTIVITY_NOT_FOUND));

        if (activity.isDeleted()) {
            throw new CustomException(ActivityErrorCode.DELETED_ACTIVITY_NOT_LINKABLE);
        }

        return activity;
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }
}
