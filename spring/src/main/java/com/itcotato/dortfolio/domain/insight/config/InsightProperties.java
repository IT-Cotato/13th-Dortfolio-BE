package com.itcotato.dortfolio.domain.insight.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "insight")
public record InsightProperties(
        int minimumAnalyzedRecordCount,
        Duration regenerationCooldown,
        int recommendationCandidateLimit,
        String embeddingModel,
        int recommendationMaxAttempts,
        Duration recommendationTimeout
) {

    public InsightProperties {
        if (minimumAnalyzedRecordCount <= 0) {
            throw new IllegalArgumentException(
                    "minimumAnalyzedRecordCount must be positive"
            );
        }

        if (regenerationCooldown == null
                || regenerationCooldown.isZero()
                || regenerationCooldown.isNegative()) {
            throw new IllegalArgumentException(
                    "regenerationCooldown must be positive"
            );
        }

        if (recommendationCandidateLimit <= 0) {
            throw new IllegalArgumentException(
                    "recommendationCandidateLimit must be positive"
            );
        }

        if (embeddingModel == null || embeddingModel.isBlank()) {
            throw new IllegalArgumentException(
                    "embeddingModel must not be blank"
            );
        }

        if (recommendationMaxAttempts <= 0) {
            throw new IllegalArgumentException(
                    "recommendationMaxAttempts must be positive"
            );
        }

        if (recommendationTimeout == null
                || recommendationTimeout.isZero()
                || recommendationTimeout.isNegative()) {
            throw new IllegalArgumentException(
                    "recommendationTimeout must be positive"
            );
        }
    }
}