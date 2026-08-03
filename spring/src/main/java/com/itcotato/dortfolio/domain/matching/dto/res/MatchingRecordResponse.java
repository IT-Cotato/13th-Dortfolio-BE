package com.itcotato.dortfolio.domain.matching.dto.res;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MatchingRecordResponse(
	UUID recordId,
	String recordTitle,
	UUID activityId,
	String activityType,
	String activityTitle,
	String templateTitle,
	LocalDateTime recordDate,
	int matchRate,
	MatchingSource matchingSource,
	String summary,
	List<MatchingRecordAnswerResponse> answers
) {

	public static MatchingRecordResponse of(
		UUID recordId,
		String recordTitle,
		UUID activityId,
		String activityType,
		String activityTitle,
		String templateTitle,
		LocalDateTime recordDate,
		int matchRate,
		MatchingSource matchingSource,
		String summary,
		List<MatchingRecordAnswerResponse> answers
	) {
		return new MatchingRecordResponse(
			recordId,
			recordTitle,
			activityId,
			activityType,
			activityTitle,
			templateTitle,
			recordDate,
			matchRate,
			matchingSource,
			summary,
			answers
		);
	}
}
