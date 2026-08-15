package com.itcotato.dortfolio.domain.record.dto.res;

import com.itcotato.dortfolio.domain.activity.entity.Activity;
import com.itcotato.dortfolio.domain.record.entity.RecordMemo;
import java.time.LocalDateTime;
import java.util.UUID;

public record RecordMemoResponse(
	UUID memoId,
	UUID activityId,
	String title,
	String content,
	boolean important,
	int sortOrder,
	boolean collapsed,
	LocalDateTime createdAt,
	LocalDateTime expiresAt
) {
	public static RecordMemoResponse from(RecordMemo recordMemo) {
		// 활동 태그는 선택 입력이라(기능명세서 3.1.5) 없는 메모가 있다
		Activity activity = recordMemo.getMemo().getActivity();

		return new RecordMemoResponse(
			recordMemo.getMemo().getId(),
			activity == null ? null : activity.getId(),
			recordMemo.getMemo().getTitle(),
			recordMemo.getMemo().getContent(),
			recordMemo.getMemo().isImportant(),
			recordMemo.getSortOrder(),
			recordMemo.isCollapsed(),
			recordMemo.getMemo().getCreatedAt(),
			recordMemo.getMemo().getExpiresAt()
		);
	}
}
