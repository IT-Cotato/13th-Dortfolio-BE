package com.itcotato.dortfolio.domain.record.analysis.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
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
	int asyncQueueCapacity
) {

	@AssertTrue(message = "asyncMaxPoolSize must be greater than or equal to asyncCorePoolSize")
	public boolean isAsyncMaxPoolSizeValid() {
		return asyncMaxPoolSize >= asyncCorePoolSize;
	}
}
