package com.itcotato.dortfolio.domain.matching.dto.res;

import java.util.List;

public record MatchingQuestionTagsResponse(
	List<MatchingQuestionTagResponse> tags
) {

	public static MatchingQuestionTagsResponse of(List<MatchingQuestionTagResponse> tags) {
		return new MatchingQuestionTagsResponse(tags);
	}
}
