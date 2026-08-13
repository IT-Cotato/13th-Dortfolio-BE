package com.itcotato.dortfolio.domain.record.dto.req;

import com.itcotato.dortfolio.domain.record.entity.RecordStatus;
import java.time.LocalDate;
import java.util.UUID;

public record RecordSearchCondition(
	UUID activityId,
	UUID templateId,
	RecordStatus status,
	LocalDate startDate,
	LocalDate endDate
) {
	public static RecordSearchCondition of(UUID activityId, UUID templateId, RecordStatus status) {
		return of(activityId, templateId, status, null, null);
	}

	public static RecordSearchCondition of(
		UUID activityId,
		UUID templateId,
		RecordStatus status,
		LocalDate startDate,
		LocalDate endDate
	) {
		return new RecordSearchCondition(activityId, templateId, status, startDate, endDate);
	}
}
