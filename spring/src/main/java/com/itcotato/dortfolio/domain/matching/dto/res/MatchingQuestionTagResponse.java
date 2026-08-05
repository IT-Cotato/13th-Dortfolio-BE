package com.itcotato.dortfolio.domain.matching.dto.res;

public record MatchingQuestionTagResponse(
	String id,
	String content
) {

	public static MatchingQuestionTagResponse of(String id, String content) {
		return new MatchingQuestionTagResponse(id, content);
	}
}
