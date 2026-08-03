package com.itcotato.dortfolio.domain.matching.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "matching")
public record MatchingProperties(
	@Min(1)
	int defaultRecordLimit,
	@Min(1)
	int maxRecordLimit,
	@Min(1)
	int candidateRecordLimit,
	@Min(1)
	int embeddingDimension,
	@NotNull
	List<String> indexedEmbeddingModels,
	@Min(1)
	int questionMaxLength,
	@Min(0)
	int minMatchRate,
	@Min(1)
	int dailyAiRequestLimit,
	@NotNull
	Duration dailyAiRequestTtl,
	@NotNull
	List<QuestionTag> questionTags
) {

	@AssertTrue(message = "maxRecordLimit must be greater than or equal to defaultRecordLimit")
	public boolean isRecordLimitValid() {
		return maxRecordLimit >= defaultRecordLimit;
	}

	@AssertTrue(message = "candidateRecordLimit must be greater than or equal to maxRecordLimit")
	public boolean isCandidateRecordLimitValid() {
		return candidateRecordLimit >= maxRecordLimit;
	}

	@AssertTrue(message = "indexedEmbeddingModels must not be empty")
	public boolean isIndexedEmbeddingModelsValid() {
		return indexedEmbeddingModels != null && !indexedEmbeddingModels.isEmpty();
	}

	@AssertTrue(message = "dailyAiRequestTtl must be positive")
	public boolean isDailyAiRequestTtlValid() {
		return dailyAiRequestTtl != null && !dailyAiRequestTtl.isZero() && !dailyAiRequestTtl.isNegative();
	}

	@AssertTrue(message = "questionTags must not be empty")
	public boolean isQuestionTagsValid() {
		return questionTags != null && !questionTags.isEmpty();
	}

	public record QuestionTag(
		@NotNull
		String id,
		@NotNull
		String content
	) {
	}
}
