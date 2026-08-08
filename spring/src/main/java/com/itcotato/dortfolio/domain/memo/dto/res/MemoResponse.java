package com.itcotato.dortfolio.domain.memo.dto.res;

import com.itcotato.dortfolio.domain.memo.entity.Memo;
import com.itcotato.dortfolio.domain.memo.entity.MemoImage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

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
    /**
     * @param downloadUrlResolver s3Key로 조회용 URL을 만들어주는 함수.
     *                            버킷이 비공개라 URL이 매번 새로 발급돼야 해서 외부에서 주입받는다.
     */
    public static MemoResponse from(
            Memo memo,
            List<MemoImage> memoImages,
            Function<String, String> downloadUrlResolver
    ) {
        // 시:분까지 계산하면 버림 처리로 D-30이 D-29로 보이므로 날짜 기준으로 계산
        long remainingDays = ChronoUnit.DAYS.between(LocalDate.now(), memo.getExpiresAt().toLocalDate());
        boolean hasActivity = memo.getActivity() != null;

        List<MemoImageResponse> images = memoImages.stream()
                .map(memoImage -> MemoImageResponse.from(memoImage, downloadUrlResolver.apply(memoImage.getS3Key())))
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
