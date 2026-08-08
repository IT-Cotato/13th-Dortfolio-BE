package com.itcotato.dortfolio.domain.record.dto.res;

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
		return new RecordMemoResponse(
			recordMemo.getMemo().getId(),
			recordMemo.getMemo().getActivity().getId(),
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
