package com.itcotato.dortfolio.domain.matching.dto.res;

import java.util.List;

public record RecordMatchingResponse(
	String question,
	MatchingSource matchingSource,
	List<MatchingRecordResponse> matchedRecords
) {

	public static RecordMatchingResponse of(
		String question,
		MatchingSource matchingSource,
		List<MatchingRecordResponse> matchedRecords
	) {
		return new RecordMatchingResponse(question, matchingSource, matchedRecords);
	}
}
