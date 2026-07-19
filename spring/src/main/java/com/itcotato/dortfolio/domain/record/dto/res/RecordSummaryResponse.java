package com.itcotato.dortfolio.domain.record.dto.res;

import com.itcotato.dortfolio.domain.record.entity.Record;
import java.time.LocalDateTime;
import java.util.UUID;

public record RecordSummaryResponse(
	UUID id,
	UUID activityId,
	String activityTitle,
	UUID templateId,
	String templateTitle,
	String title,
	String status,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	LocalDateTime completedAt
) {
	public static RecordSummaryResponse from(Record record) {
		return new RecordSummaryResponse(
			record.getId(),
			record.getActivity().getId(),
			record.getActivity().getTitle(),
			record.getTemplate().getId(),
			record.getTemplate().getTitle(),
			record.getTitle(),
			record.getStatus().name(),
			record.getCreatedAt(),
			record.getUpdatedAt(),
			record.getCompletedAt()
		);
	}
}
