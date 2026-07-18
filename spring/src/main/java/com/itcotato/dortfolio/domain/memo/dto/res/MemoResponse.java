package com.itcotato.dortfolio.domain.memo.dto.res;

import com.itcotato.dortfolio.domain.memo.entity.Memo;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
        LocalDateTime createdAt
) {
    public static MemoResponse from(Memo memo) {
        long remainingDays = ChronoUnit.DAYS.between(LocalDateTime.now(), memo.getExpiresAt());
        boolean hasActivity = memo.getActivity() != null;

        return new MemoResponse(
                memo.getId(),
                hasActivity ? memo.getActivity().getId() : null,
                hasActivity ? memo.getActivity().getTitle() : null,
                memo.getTitle(),
                memo.getContent(),
                memo.getColor(),
                memo.isImportant(),
                Math.max(remainingDays, 0),
                memo.getCreatedAt()
        );
    }
}
