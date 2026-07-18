package com.itcotato.dortfolio.domain.record.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "record")
public record RecordProperties(
	int deleteGracePeriodDays,
	int recentRecordLimit
) {
}
