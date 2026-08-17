package com.itcotato.dortfolio.domain.insight.recommendation.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.itcotato.dortfolio.domain.insight.config.InsightProperties;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcRecommendationCandidateQueryTest {

    @Test
    void rejectsNonFiniteMinimumSimilarity() {
        JdbcRecommendationCandidateQuery query =
                new JdbcRecommendationCandidateQuery(
                        new JdbcTemplate(),
                        properties()
                );

        assertThatThrownBy(() -> query.findTopCandidates(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDateTime.now(),
                1,
                Double.NaN
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minimumSimilarity");
    }

    @Test
    void candidateSqlContainsInsightEligibilityAndDeterministicOrder() {
        JdbcRecommendationCandidateQuery query =
                new JdbcRecommendationCandidateQuery(
                        new JdbcTemplate(),
                        properties()
                );

        String sql = query.buildCandidateSql(
                "gemini-embedding-2"
        );

        assertThat(sql)
                .contains("record.user_id = ?")
                .contains("record.status = 'COMPLETED'")
                .contains("record.deleted_at is null")
                .contains("activity.deleted_at is null")
                .doesNotContain("template.deleted_at is null")
                .contains("record.completed_at <= ?")
                .contains("analysis.ai_analysis_status = 'COMPLETED'")
                .contains("analysis.analyzed_record_updated_at")
                .contains("record_embedding.embedding_model = 'gemini-embedding-2'")
                .contains(") >= ?")
                .contains("record.id asc")
                .contains("limit ?");
    }

    @Test
    void candidateSqlEscapesConfiguredEmbeddingModel() {
        JdbcRecommendationCandidateQuery query =
                new JdbcRecommendationCandidateQuery(
                        new JdbcTemplate(),
                        properties()
                );

        assertThat(query.buildCandidateSql("model'value"))
                .contains("embedding_model = 'model''value'");
    }

    private InsightProperties properties() {
        return new InsightProperties(
                10,
                Duration.ofHours(24),
                0.1,
                1,
                5,
                0.0,
                "gemini-embedding-2",
                2,
                Duration.ofSeconds(10)
        );
    }
}
