package com.itcotato.dortfolio.domain.job.embedding.service;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "job-competency-embedding.request")
public record JobCompetencyEmbeddingRequestProperties(
        Duration interval,
        int maxAttempts,
        Duration initialBackoff
) {

    public JobCompetencyEmbeddingRequestProperties {
        if (interval == null || interval.isNegative()) {
            throw new IllegalArgumentException("interval must not be negative");
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        if (initialBackoff == null || initialBackoff.isNegative()) {
            throw new IllegalArgumentException("initialBackoff must not be negative");
        }
    }
}
