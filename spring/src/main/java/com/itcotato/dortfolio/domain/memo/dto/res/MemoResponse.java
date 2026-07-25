package com.itcotato.dortfolio.domain.memo.dto.res;

import com.itcotato.dortfolio.domain.memo.entity.Memo;
import com.itcotato.dortfolio.domain.memo.entity.MemoImage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

public record MemoResponse(
        UUID id,
        UUID activityId,
        String activityTitle,
        String title,
        String content,
        String color,
        boolean isImportant,
        long remainingDaysUntilExpiration,
        List<MemoImageResponse> images,
        int imageCount,
        LocalDateTime createdAt
) {
    public static MemoResponse from(Memo memo, List<MemoImage> memoImages) {
        // 시:분까지 계산하면 버림 처리로 D-30이 D-29로 보이므로 날짜 기준으로 계산
        long remainingDays = ChronoUnit.DAYS.between(LocalDate.now(), memo.getExpiresAt().toLocalDate());
        boolean hasActivity = memo.getActivity() != null;

        List<MemoImageResponse> images = memoImages.stream()
                .map(MemoImageResponse::from)
                .toList();

        return new MemoResponse(
                memo.getId(),
                hasActivity ? memo.getActivity().getId() : null,
                hasActivity ? memo.getActivity().getTitle() : null,
                memo.getTitle(),
                memo.getContent(),
                memo.getColor(),
                memo.isImportant(),
                Math.max(remainingDays, 0),
                images,
                images.size(),
                memo.getCreatedAt()
        );
    }
}
