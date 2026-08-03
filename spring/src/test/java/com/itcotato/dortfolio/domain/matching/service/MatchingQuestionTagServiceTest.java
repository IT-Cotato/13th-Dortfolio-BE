package com.itcotato.dortfolio.domain.matching.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.itcotato.dortfolio.domain.matching.config.MatchingProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class MatchingQuestionTagServiceTest {

	@Test
	void getQuestionTagsReturnsConfiguredTags() {
		MatchingQuestionTagService service = new MatchingQuestionTagService(
			new MatchingProperties(
				3,
				10,
				10,
				3072,
				List.of("test-embedding"),
				500,
				30,
				20,
				Duration.ofDays(1),
				List.of(new MatchingProperties.QuestionTag("TEST", "테스트 문항"))
			)
		);

		assertThat(service.getQuestionTags().tags())
			.singleElement()
			.satisfies(tag -> {
				assertThat(tag.id()).isEqualTo("TEST");
				assertThat(tag.content()).isEqualTo("테스트 문항");
			});
	}
}
