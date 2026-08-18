package com.itcotato.dortfolio.domain.record.analysis.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "record-analysis")
public record RecordAnalysisProperties(
	boolean enabled,
	@Min(1)
	int asyncCorePoolSize,
	@Min(1)
	int asyncMaxPoolSize,
	@Min(0)
	int asyncQueueCapacity,
	@NotNull
	Duration lockTimeout,
	@Min(1)
	int strengthCandidateLimit,
	@Min(1)
	int strengthMaxCount,
	double strengthMinSimilarity,
	@Min(1)
	int embeddingMaxCharacters
) {

	@AssertTrue(message = "asyncMaxPoolSize must be greater than or equal to asyncCorePoolSize")
	public boolean isAsyncMaxPoolSizeValid() {
		return asyncMaxPoolSize >= asyncCorePoolSize;
	}

	@AssertTrue(message = "lockTimeout must be positive")
	public boolean isLockTimeoutValid() {
		return lockTimeout != null && !lockTimeout.isZero() && !lockTimeout.isNegative();
	}

	@AssertTrue(message = "strengthMaxCount must not exceed strengthCandidateLimit")
	public boolean isStrengthCountValid() {
		return strengthMaxCount <= strengthCandidateLimit;
	}

	@AssertTrue(message = "strengthMinSimilarity must be finite and between -1.0 and 1.0")
	public boolean isStrengthMinSimilarityValid() {
		return Double.isFinite(strengthMinSimilarity)
			&& strengthMinSimilarity >= -1.0
			&& strengthMinSimilarity <= 1.0;
	}
}
