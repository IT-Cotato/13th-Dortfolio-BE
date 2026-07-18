package com.itcotato.dortfolio.domain.record.dto.res;

import com.itcotato.dortfolio.domain.record.entity.Record;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RecordResponse(
	UUID id,
	UUID activityId,
	String activityTitle,
	UUID templateId,
	String templateTitle,
	String title,
	String status,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	LocalDateTime completedAt,
	List<RecordAnswerResponse> answers,
	List<RecordMemoResponse> memos
) {
	public static RecordResponse of(
		Record record,
		List<RecordAnswerResponse> answers,
		List<RecordMemoResponse> memos
	) {
		return new RecordResponse(
			record.getId(),
			record.getActivity().getId(),
			record.getActivity().getTitle(),
			record.getTemplate().getId(),
			record.getTemplate().getTitle(),
			record.getTitle(),
			record.getStatus().name(),
			record.getCreatedAt(),
			record.getUpdatedAt(),
			record.getCompletedAt(),
			answers,
			memos
		);
	}
}
