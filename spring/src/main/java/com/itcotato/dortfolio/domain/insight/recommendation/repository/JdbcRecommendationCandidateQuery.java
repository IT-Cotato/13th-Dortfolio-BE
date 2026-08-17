package com.itcotato.dortfolio.domain.insight.recommendation.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itcotato.dortfolio.domain.insight.config.InsightProperties;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationCandidate;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.JobErrorCode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class JdbcRecommendationCandidateQuery
        implements RecommendationCandidateQuery {

    private static final int EMBEDDING_DIMENSION = 3072;

    private static final TypeReference<List<String>>
            STRING_LIST_TYPE = new TypeReference<>() {};

    private final JdbcTemplate jdbcTemplate;
    private final InsightProperties insightProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationCandidate> findTopCandidates(
            UUID userId,
            UUID jobCompetencyId,
            LocalDateTime snapshotAt,
            int limit,
            double minimumSimilarity
    ) {
        validateArguments(
                userId,
                jobCompetencyId,
                snapshotAt,
                limit,
                minimumSimilarity
        );

        String embeddingModel = insightProperties.embeddingModel();

        String competencyVector = findCompetencyVector(
                jobCompetencyId,
                embeddingModel
        );

        jdbcTemplate.execute(
                "set local hnsw.iterative_scan = strict_order"
        );

        return jdbcTemplate.query(
                buildCandidateSql(embeddingModel),
                this::mapCandidate,

                // SELECT 절의 유사도 계산에 사용
                competencyVector,

                userId,
                snapshotAt,
                snapshotAt,
                snapshotAt,

                // 최소 유사도 필터 계산에 사용
                competencyVector,
                minimumSimilarity,

                // ORDER BY의 거리 계산에 다시 사용
                competencyVector,

                limit
        );
    }

    private String findCompetencyVector(
            UUID jobCompetencyId,
            String embeddingModel
    ) {
        List<String> vectors = jdbcTemplate.query(
                """
                select embedding::text as embedding_value
                from job_competency_embeddings
                where job_competency_id = ?
                  and embedding_model = ?
                """,
                (resultSet, rowNumber) ->
                        resultSet.getString("embedding_value"),
                jobCompetencyId,
                embeddingModel
        );

        if (vectors.isEmpty()) {
            throw new CustomException(
                    JobErrorCode
                            .JOB_COMPETENCY_EMBEDDING_NOT_READY
            );
        }

        return vectors.get(0);
    }

    String buildCandidateSql(String embeddingModel) {
        String modelLiteral = toSqlLiteral(embeddingModel);

        return """
                select
                    record.id as record_id,
                    record.title as record_title,
                    template.title as template_name,
                    analysis.summary as summary,
                    analysis.evidence_snippets as evidence_snippets,
                    1.0 - (
                        record_embedding.embedding::halfvec(%d)
                        <=> cast(? as halfvec(%d))
                    ) as cosine_similarity
                from record_embeddings record_embedding
                join records record
                  on record.id = record_embedding.record_id
                join activity activity
                  on activity.id = record.activity_id
                join templates template
                  on template.id = record.template_id
                join record_analysis analysis
                  on analysis.record_id = record.id
                where record.user_id = ?
                  and record.status = 'COMPLETED'
                  and record.deleted_at is null
                  and activity.deleted_at is null
                  and record.completed_at <= ?
                  and record.updated_at <= ?
                  and analysis.ai_analysis_status = 'COMPLETED'
                  and analysis.analyzed_at <= ?
                  and analysis.analyzed_record_updated_at
                        = record.updated_at
                  and record_embedding.embedding_model = %s
                  and 1.0 - (
                        record_embedding.embedding::halfvec(%d)
                        <=> cast(? as halfvec(%d))
                  ) >= ?
                order by
                    record_embedding.embedding::halfvec(%d)
                        <=> cast(? as halfvec(%d)) asc,
                    record.id asc
                limit ?
                """.formatted(
                EMBEDDING_DIMENSION,
                EMBEDDING_DIMENSION,
                modelLiteral,
                EMBEDDING_DIMENSION,
                EMBEDDING_DIMENSION,
                EMBEDDING_DIMENSION,
                EMBEDDING_DIMENSION
        );
    }

    private RecommendationCandidate mapCandidate(
            ResultSet resultSet,
            int rowNumber
    ) throws SQLException {
        return new RecommendationCandidate(
                resultSet.getObject(
                        "record_id",
                        UUID.class
                ),
                resultSet.getString("record_title"),
                resultSet.getString("template_name"),
                resultSet.getString("summary"),
                parseEvidenceSnippets(
                        resultSet.getString(
                                "evidence_snippets"
                        )
                ),
                resultSet.getDouble("cosine_similarity")
        );
    }

    private List<String> parseEvidenceSnippets(
            String evidenceSnippets
    ) throws SQLException {
        if (evidenceSnippets == null
                || evidenceSnippets.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(
                    evidenceSnippets,
                    STRING_LIST_TYPE
            );
        } catch (JsonProcessingException exception) {
            throw new SQLException(
                    "Failed to parse evidence snippets.",
                    exception
            );
        }
    }

    private void validateArguments(
            UUID userId,
            UUID jobCompetencyId,
            LocalDateTime snapshotAt,
            int limit,
            double minimumSimilarity
    ) {
        if (userId == null) {
            throw new IllegalArgumentException(
                    "userId must not be null"
            );
        }

        if (jobCompetencyId == null) {
            throw new IllegalArgumentException(
                    "jobCompetencyId must not be null"
            );
        }

        if (snapshotAt == null) {
            throw new IllegalArgumentException(
                    "snapshotAt must not be null"
            );
        }

        if (limit <= 0
                || limit
                > insightProperties
                .recommendationCandidateMax()) {
            throw new IllegalArgumentException(
                    "Recommendation candidate limit is invalid."
            );
        }

        if (!Double.isFinite(minimumSimilarity)
                || minimumSimilarity < 0.0
                || minimumSimilarity > 1.0) {
            throw new IllegalArgumentException(
                    "minimumSimilarity must be in [0, 1]"
            );
        }
    }

    private String toSqlLiteral(String value) {
        return "'"
                + value.replace("'", "''")
                + "'";
    }
}
