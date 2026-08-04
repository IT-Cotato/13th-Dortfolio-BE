package com.itcotato.dortfolio.domain.insight.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class InsightPropertiesTest {

    @Test
    void rejectsNonPositiveCandidateLimit() {
        assertThatThrownBy(() -> new InsightProperties(
                10,
                Duration.ofHours(24),
                0,
                "gemini-embedding-2",
                2,
                Duration.ofSeconds(10)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recommendationCandidateLimit");
    }

    @Test
    void rejectsBlankEmbeddingModel() {
        assertThatThrownBy(() -> new InsightProperties(
                10,
                Duration.ofHours(24),
                5,
                " ",
                2,
                Duration.ofSeconds(10)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("embeddingModel");
    }

    @Test
    void rejectsNonPositiveRecommendationMaxAttempts() {
        assertThatThrownBy(() -> new InsightProperties(
                10,
                Duration.ofHours(24),
                5,
                "gemini-embedding-2",
                0,
                Duration.ofSeconds(10)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recommendationMaxAttempts");
    }

    @Test
    void rejectsNonPositiveRecommendationTimeout() {
        assertThatThrownBy(() -> new InsightProperties(
                10,
                Duration.ofHours(24),
                5,
                "gemini-embedding-2",
                2,
                Duration.ZERO
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recommendationTimeout");
    }
}
