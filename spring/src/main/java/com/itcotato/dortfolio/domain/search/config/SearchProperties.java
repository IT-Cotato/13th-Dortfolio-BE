package com.itcotato.dortfolio.domain.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "search")
public record SearchProperties(
		int defaultPageSize,
		int maxPageSize
) {
}
