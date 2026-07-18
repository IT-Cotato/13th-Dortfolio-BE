package com.itcotato.dortfolio.domain.record.dto.req;

import com.itcotato.dortfolio.domain.record.entity.RecordStatus;
import java.util.UUID;

public record RecordSearchCondition(
	UUID activityId,
	UUID templateId,
	RecordStatus status
) {
	public static RecordSearchCondition of(UUID activityId, UUID templateId, RecordStatus status) {
		return new RecordSearchCondition(activityId, templateId, status);
	}
}
