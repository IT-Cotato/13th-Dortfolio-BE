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

	private MatchingProperties properties(int embeddingDimension, int minMatchRate) {
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
			List.of(new MatchingProperties.QuestionTag("TEST", "테스트 문항"))
		);
	}
}
