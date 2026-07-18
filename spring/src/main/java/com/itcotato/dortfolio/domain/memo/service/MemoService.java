package com.itcotato.dortfolio.domain.memo.service;

import com.itcotato.dortfolio.domain.activity.entity.Activity;
import com.itcotato.dortfolio.domain.activity.repository.ActivityRepository;
import com.itcotato.dortfolio.domain.memo.dto.req.MemoCreateRequest;
import com.itcotato.dortfolio.domain.memo.dto.res.MemoResponse;
import com.itcotato.dortfolio.domain.memo.dto.req.MemoUpdateRequest;
import com.itcotato.dortfolio.domain.memo.entity.Memo;
import com.itcotato.dortfolio.domain.memo.repository.MemoRepository;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemoService {

    private static final int DELETE_GRACE_PERIOD_DAYS = 30;
    private static final int DEFAULT_SORT_ORDER = 0;

    private final MemoRepository memoRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    @Transactional
    public UUID createMemo(UUID userId, MemoCreateRequest request) {
        User user = getUserOrThrow(userId);
        Activity activity = resolveActivityOrNull(request.activityId(), userId);

        Memo memo = Memo.create(
                user,
                activity,
                request.title(),
                request.content(),
                request.color(),
                DEFAULT_SORT_ORDER
        );

        return memoRepository.save(memo).getId();
    }

    public List<MemoResponse> getMemos(UUID userId) {
        return memoRepository.findAllByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(userId).stream()
                .map(MemoResponse::from)
                .toList();
    }

    public MemoResponse getMemo(UUID userId, UUID memoId) {
        return MemoResponse.from(getMemoOrThrow(memoId, userId));
    }

    @Transactional
    public void updateMemo(UUID userId, UUID memoId, MemoUpdateRequest request) {
        Memo memo = getMemoOrThrow(memoId, userId);
        Activity activity = resolveActivityOrNull(request.activityId(), userId);

        memo.update(activity, request.title(), request.content(), request.color());
    }

    @Transactional
    public void markImportant(UUID userId, UUID memoId, boolean important) {
        getMemoOrThrow(memoId, userId).markImportant(important);
    }

    @Transactional
    public void deleteMemo(UUID userId, UUID memoId) {
        getMemoOrThrow(memoId, userId).markDeleted(DELETE_GRACE_PERIOD_DAYS);
    }

    @Transactional
    public void restoreMemo(UUID userId, UUID memoId) {
        getMemoOrThrow(memoId, userId).restore();
    }

    private Memo getMemoOrThrow(UUID memoId, UUID userId) {
        return memoRepository.findByIdAndUser_Id(memoId, userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메모입니다."));
    }

    private Activity resolveActivityOrNull(UUID activityId, UUID userId) {
        if (activityId == null) {
            return null;
        }
        return activityRepository.findByIdAndUser_Id(activityId, userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 활동입니다."));
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    }
}
