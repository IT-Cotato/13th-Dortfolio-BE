package com.itcotato.dortfolio.domain.insight.recommendation.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.itcotato.dortfolio.domain.insight.config.InsightProperties;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationCandidate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class JdbcRecommendationCandidateQueryContainerTest {

    private static final String MODEL = "gemini-embedding-2";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    DockerImageName.parse("pgvector/pgvector:pg16")
                            .asCompatibleSubstituteFor("postgres")
            );

    private static JdbcTemplate jdbcTemplate;
    private static TransactionTemplate transactionTemplate;
    private static JdbcRecommendationCandidateQuery repository;

    @BeforeAll
    static void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        );
        dataSource.setDriverClassName(POSTGRES.getDriverClassName());

        FluentConfiguration flywayConfiguration = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration");
        flywayConfiguration.getConfigurationExtension(PostgreSQLConfigurationExtension.class)
                .setTransactionalLock(false);
        flywayConfiguration.load().migrate();

        jdbcTemplate = new JdbcTemplate(dataSource);
        transactionTemplate = new TransactionTemplate(
                new DataSourceTransactionManager(dataSource)
        );
        repository = new JdbcRecommendationCandidateQuery(
                jdbcTemplate,
                new InsightProperties(
                        10,
                        Duration.ofHours(24),
                        5,
                        MODEL,
                        2,
                        Duration.ofSeconds(10)
                )
        );
    }

    @Test
    void excludesAnotherUsersAndPostSnapshotRecords() {
        UUID ownerId = insertUser("owner");
        UUID anotherUserId = insertUser("another");
        UUID competencyId = insertCompetencyEmbedding();
        LocalDateTime snapshotAt =
                LocalDateTime.of(2026, 8, 7, 10, 0);

        UUID expectedRecordId = insertAnalyzedRecord(
                ownerId,
                "소유자의 기준 시각 이전 기록",
                snapshotAt.minusHours(1)
        );
        UUID boundaryRecordId = insertAnalyzedRecord(
                ownerId,
                "소유자의 기준 시각과 같은 기록",
                snapshotAt
        );
        insertAnalyzedRecord(
                ownerId,
                "소유자의 기준 시각 이후 기록",
                snapshotAt.plusSeconds(1)
        );
        insertAnalyzedRecord(
                anotherUserId,
                "다른 사용자의 기록",
                snapshotAt.minusHours(1)
        );

        List<RecommendationCandidate> result =
                transactionTemplate.execute(status ->
                        repository.findTopCandidates(
                                ownerId,
                                competencyId,
                                snapshotAt,
                                5
                        )
                );

        assertThat(result)
                .extracting(RecommendationCandidate::recordId)
                .containsExactlyInAnyOrder(
                        boundaryRecordId,
                        expectedRecordId
                );
    }

    private static UUID insertUser(String prefix) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into users (
                    id, created_at, updated_at, email, nickname, role
                ) values (?, now(), now(), ?, ?, 'USER')
                """, id, prefix + "-" + id + "@test.com", prefix);
        return id;
    }

    private static UUID insertCompetencyEmbedding() {
        UUID jobId = UUID.randomUUID();
        UUID tagId = UUID.randomUUID();
        UUID competencyId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into jobs (
                    id, created_at, updated_at,
                    code, category_code, name
                ) values (
                    ?, now(), now(),
                    'TEST_JOB_001', 'IT_DEVELOPMENT', '백엔드 개발자'
                )
                """, jobId);
        jdbcTemplate.update("""
                insert into competency_tags (
                    id, created_at, updated_at, code, name
                ) values (
                    ?, now(), now(), 'TEST_COMP_001', '문제 해결'
                )
                """, tagId);
        jdbcTemplate.update("""
                insert into job_competencies (
                    id, created_at, updated_at, sort_order,
                    competency_tag_id, job_id
                ) values (?, now(), now(), 1, ?, ?)
                """, competencyId, tagId, jobId);
        jdbcTemplate.update("""
                insert into job_competency_embeddings (
                    id, created_at, updated_at, job_competency_id,
                    embedding_model, embedding
                ) values (?, now(), now(), ?, ?, ?::vector)
                """, UUID.randomUUID(), competencyId, MODEL, vector());
        return competencyId;
    }

    private static UUID insertAnalyzedRecord(
            UUID userId,
            String title,
            LocalDateTime completedAt
    ) {
        UUID activityTypeId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        jdbcTemplate.update("""
                insert into activity_type (
                    id, created_at, updated_at, is_default, name, user_id
                ) values (?, now(), now(), false, ?, ?)
                """, activityTypeId, title, userId);
        jdbcTemplate.update("""
                insert into activity (
                    id, created_at, updated_at, is_ongoing, started_at,
                    status, title, activity_type_id, user_id
                ) values (?, now(), now(), true, current_date,
                          'IN_PROGRESS', ?, ?, ?)
                """, activityId, title, activityTypeId, userId);
        jdbcTemplate.update("""
                insert into templates (
                    id, created_at, updated_at, is_builtin, title, user_id
                ) values (?, now(), now(), false, '경험 기록', ?)
                """, templateId, userId);
        jdbcTemplate.update("""
                insert into records (
                    id, created_at, updated_at, completed_at, status,
                    title, version, activity_id, template_id, user_id
                ) values (?, ?, ?, ?, 'COMPLETED', ?, 0, ?, ?, ?)
                """,
                recordId,
                completedAt.minusHours(1),
                completedAt,
                completedAt,
                title,
                activityId,
                templateId,
                userId
        );
        jdbcTemplate.update("""
                insert into record_analysis (
                    id, created_at, updated_at, ai_analysis_status,
                    analyzed_at, analyzed_record_updated_at,
                    evidence_snippets, failure_retryable,
                    last_attempt_failed, last_failure_retryable,
                    summary, record_id
                ) values (?, now(), now(), 'COMPLETED', ?, ?,
                          '["근거 문장"]', false, false, false, ?, ?)
                """, UUID.randomUUID(), completedAt, completedAt,
                "분석 요약", recordId);
        jdbcTemplate.update("""
                insert into record_embeddings (
                    id, created_at, updated_at, embedding,
                    embedding_model, record_id
                ) values (?, now(), now(), ?::vector, ?, ?)
                """, UUID.randomUUID(), vector(), MODEL, recordId);
        return recordId;
    }

    private static String vector() {
        List<String> values =
                new java.util.ArrayList<>(
                        Collections.nCopies(3072, "0")
                );
        values.set(0, "1");
        return "[" + String.join(",", values) + "]";
    }
}
