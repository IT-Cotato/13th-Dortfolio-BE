package com.itcotato.dortfolio.domain.record.analysis.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.fastapi")
public record AiServiceProperties(
	String baseUrl,
	Duration connectTimeout,
	Duration readTimeout
) {
}
