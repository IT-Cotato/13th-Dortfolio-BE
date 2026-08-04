package com.itcotato.dortfolio.domain.matching.service;

import com.itcotato.dortfolio.domain.matching.config.MatchingProperties;
import com.itcotato.dortfolio.domain.matching.dto.res.MatchingQuestionTagResponse;
import com.itcotato.dortfolio.domain.matching.dto.res.MatchingQuestionTagsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchingQuestionTagService {

	private final MatchingProperties matchingProperties;

	public MatchingQuestionTagsResponse getQuestionTags() {
		return MatchingQuestionTagsResponse.of(
			matchingProperties.questionTags().stream()
				.map(tag -> MatchingQuestionTagResponse.of(tag.id(), tag.content()))
				.toList()
		);
	}
}
