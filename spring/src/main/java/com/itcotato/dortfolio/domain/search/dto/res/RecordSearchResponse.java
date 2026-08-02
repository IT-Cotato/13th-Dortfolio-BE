package com.itcotato.dortfolio.domain.search.dto.res;

import com.itcotato.dortfolio.domain.record.entity.Record;
import java.time.LocalDateTime;
import java.util.UUID;

// 기능명세서 5.3.1.1 검색 결과 출력 항목: 활동 종류 태그, 기록 제목, 기록 날짜, 기록 내용, 템플릿 제목 태그
public record RecordSearchResponse(
		UUID recordId,
		String activityTypeName,
		String activityTitle,
		String templateTitle,
		String title,
		String content,
		LocalDateTime createdAt
) {
	public static RecordSearchResponse of(Record record, String content) {
		return new RecordSearchResponse(
				record.getId(),
				record.getActivity().getActivityType().getName(),
				record.getActivity().getTitle(),
				record.getTemplate().getTitle(),
				record.getTitle(),
				content,
				record.getCreatedAt()
		);
	}
}
