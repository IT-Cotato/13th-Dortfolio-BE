package com.itcotato.dortfolio.domain.story.dto.res;

import com.itcotato.dortfolio.domain.activity.entity.Activity;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// 기능명세서 5.1 타임라인 표시 항목: 활동 제목, 활동 기간, 활동 종류
public record TimelineActivityResponse(
		UUID activityId,
		String title,
		String activityTypeName,
		LocalDate startedAt,
		LocalDate endedAt,
		boolean isOngoing,
		int recordCount,
		List<TimelineRecordResponse> records
) {
	public static TimelineActivityResponse of(Activity activity, List<TimelineRecordResponse> records) {
		return new TimelineActivityResponse(
				activity.getId(),
				activity.getTitle(),
				activity.getActivityType().getName(),
				activity.getStartedAt(),
				activity.getEndedAt(),
				activity.isOngoing(),
				records.size(),
				records
		);
	}
}
