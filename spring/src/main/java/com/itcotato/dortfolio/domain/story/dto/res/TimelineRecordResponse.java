package com.itcotato.dortfolio.domain.story.dto.res;

import com.itcotato.dortfolio.domain.record.entity.Record;
import java.time.LocalDateTime;
import java.util.UUID;

// 기능명세서 5.1.1 개별 기록 리스트 표시 항목: 기록 제목, 기록 날짜
public record TimelineRecordResponse(
		UUID recordId,
		String title,
		String status,
		LocalDateTime createdAt
) {
	public static TimelineRecordResponse from(Record record) {
		return new TimelineRecordResponse(
				record.getId(),
				record.getTitle(),
				record.getStatus().name(),
				record.getCreatedAt()
		);
	}
}
