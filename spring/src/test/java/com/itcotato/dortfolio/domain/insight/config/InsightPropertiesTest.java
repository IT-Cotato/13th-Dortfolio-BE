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
                "gemini-embedding-2"
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
                " "
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("embeddingModel");
    }
}
