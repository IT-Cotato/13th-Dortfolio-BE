package com.itcotato.dortfolio.domain.record.analysis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "record-analysis")
public record RecordAnalysisProperties(
	boolean enabled,
	int asyncCorePoolSize,
	int asyncMaxPoolSize,
	int asyncQueueCapacity
) {
}
