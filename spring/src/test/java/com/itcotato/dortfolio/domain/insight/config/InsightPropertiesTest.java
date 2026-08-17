package com.itcotato.dortfolio.domain.insight.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class InsightPropertiesTest {

    @Test
    void rejectsCandidateRatioAboveOne() {
        assertThatThrownBy(() -> new InsightProperties(
                10,
                Duration.ofHours(24),
                1.1,
                1,
                20,
                0.0,
                "gemini-embedding-2",
                2,
                Duration.ofSeconds(10)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recommendationCandidateRatio");
    }

    @Test
    void rejectsCandidateMaximumBelowMinimum() {
        assertThatThrownBy(() -> new InsightProperties(
                10,
                Duration.ofHours(24),
                0.1,
                5,
                4,
                0.0,
                "gemini-embedding-2",
                2,
                Duration.ofSeconds(10)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recommendationCandidateMax");
    }

    @Test
    void rejectsSimilarityAboveOne() {
        assertThatThrownBy(() -> new InsightProperties(
                10,
                Duration.ofHours(24),
                0.1,
                1,
                20,
                1.1,
                "gemini-embedding-2",
                2,
                Duration.ofSeconds(10)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recommendationMinSimilarity");
    }

    @Test
    void rejectsNonPositiveCandidateMinimum() {
        assertThatThrownBy(() -> new InsightProperties(
                10,
                Duration.ofHours(24),
                0.1,
                0,
                20,
                0.0,
                "gemini-embedding-2",
                2,
                Duration.ofSeconds(10)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recommendationCandidateMin");
    }

    @Test
    void rejectsBlankEmbeddingModel() {
        assertThatThrownBy(() -> new InsightProperties(
                10,
                Duration.ofHours(24),
                0.1,
                1,
                5,
                0.0,
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
                0.1,
                1,
                5,
                0.0,
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
                0.1,
                1,
                5,
                0.0,
                "gemini-embedding-2",
                2,
                Duration.ZERO
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recommendationTimeout");
    }
}
