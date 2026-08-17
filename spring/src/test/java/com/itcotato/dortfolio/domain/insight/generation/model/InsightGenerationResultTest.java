package com.itcotato.dortfolio.domain.insight.generation.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.itcotato.dortfolio.domain.insight.generation.model.InsightGenerationResult.JobRecommendationResult;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InsightGenerationResultTest {

	@Test
	void rejectsMatchedRecommendationWithoutRecordDetails() {
		assertThatThrownBy(() -> new JobRecommendationResult(
			true,
			UUID.randomUUID(),
			"문제 해결",
			null,
			"기록",
			"템플릿",
			"이유",
			0.9
		)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsUnmatchedRecommendationWithRecordDetails() {
		assertThatThrownBy(() -> new JobRecommendationResult(
			false,
			UUID.randomUUID(),
			"문제 해결",
			UUID.randomUUID(),
			"기록",
			"템플릿",
			"이유",
			0.9
		)).isInstanceOf(IllegalArgumentException.class);
	}
}
