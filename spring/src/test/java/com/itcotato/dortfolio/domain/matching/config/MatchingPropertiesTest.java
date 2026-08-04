package com.itcotato.dortfolio.domain.matching.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class MatchingPropertiesTest {

	@Test
	void validationRejectsMatchRateGreaterThanOneHundred() {
		MatchingProperties properties = properties(3072, 101);

		var violations = Validation.buildDefaultValidatorFactory()
			.getValidator()
			.validate(properties);

		assertThat(violations)
			.extracting(violation -> violation.getPropertyPath().toString())
			.contains("minMatchRate");
	}

	@Test
	void validationRejectsEmbeddingDimensionThatDiffersFromVectorIndex() {
		MatchingProperties properties = properties(1536, 30);

		var violations = Validation.buildDefaultValidatorFactory()
			.getValidator()
			.validate(properties);

		assertThat(violations)
			.extracting(violation -> violation.getPropertyPath().toString())
			.contains("embeddingDimensionValid");
	}

	@Test
	void validationRejectsBlankQuestionTagFields() {
		MatchingProperties properties = properties(
			3072,
			30,
			List.of(new MatchingProperties.QuestionTag("", " "))
		);

		var violations = Validation.buildDefaultValidatorFactory()
			.getValidator()
			.validate(properties);

		assertThat(violations)
			.extracting(violation -> violation.getPropertyPath().toString())
			.contains("questionTags[0].id", "questionTags[0].content");
	}

	private MatchingProperties properties(int embeddingDimension, int minMatchRate) {
		return properties(
			embeddingDimension,
			minMatchRate,
			List.of(new MatchingProperties.QuestionTag("TEST", "테스트 문항"))
		);
	}

	private MatchingProperties properties(
		int embeddingDimension,
		int minMatchRate,
		List<MatchingProperties.QuestionTag> questionTags
	) {
		return new MatchingProperties(
			3,
			10,
			10,
			embeddingDimension,
			List.of("gemini-embedding-2"),
			500,
			minMatchRate,
			20,
			Duration.ofDays(1),
			ZoneId.of("Asia/Seoul"),
			questionTags
		);
	}
}
