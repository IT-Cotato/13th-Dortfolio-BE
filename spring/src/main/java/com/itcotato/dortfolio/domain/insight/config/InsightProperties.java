package com.itcotato.dortfolio.domain.insight.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "insight")
public record InsightProperties(
        int minimumAnalyzedRecordCount,
        Duration regenerationCooldown,
        double recommendationCandidateRatio,
        int recommendationCandidateMin,
        int recommendationCandidateMax,
        double recommendationMinSimilarity,
        String embeddingModel,
        int recommendationMaxAttempts,
        Duration recommendationInitialBackoff,
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

        if (!Double.isFinite(recommendationCandidateRatio)
                || recommendationCandidateRatio <= 0.0
                || recommendationCandidateRatio > 1.0) {
            throw new IllegalArgumentException(
                    "recommendationCandidateRatio must be in (0, 1]"
            );
        }

        if (recommendationCandidateMin <= 0) {
            throw new IllegalArgumentException(
                    "recommendationCandidateMin must be positive"
            );
        }

        if (recommendationCandidateMax < recommendationCandidateMin) {
            throw new IllegalArgumentException(
                    "recommendationCandidateMax must be greater than or equal to recommendationCandidateMin"
            );
        }

        if (!Double.isFinite(recommendationMinSimilarity)
                || recommendationMinSimilarity < 0.0
                || recommendationMinSimilarity > 1.0) {
            throw new IllegalArgumentException(
                    "recommendationMinSimilarity must be in [0, 1]"
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

        if (recommendationInitialBackoff == null
                || recommendationInitialBackoff.isNegative()) {
            throw new IllegalArgumentException(
                    "recommendationInitialBackoff must not be null or negative"
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
