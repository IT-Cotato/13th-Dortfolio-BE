package com.itcotato.dortfolio.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationTest {

	@Container
	private final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
		DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres")
	);

	@Test
	void migratesFreshSchemaThroughLatestVersion() {
		MigrateResult result = flyway().migrate();

		assertThat(result.migrationsExecuted).isEqualTo(8);
		assertThat(result.targetSchemaVersion).isEqualTo("8");
		assertMatchingIndexesCreated();
		assertRunningInsightStatusAllowed();
		assertRecordAnswersNormalized();
	}

	private Flyway flyway() {
		return Flyway.configure()
			.dataSource(dataSource())
			.locations("classpath:db/migration")
			.baselineOnMigrate(true)
			.baselineVersion(MigrationVersion.fromVersion("0"))
			.load();
	}

	private DriverManagerDataSource dataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(
			postgres.getJdbcUrl(),
			postgres.getUsername(),
			postgres.getPassword()
		);
		dataSource.setDriverClassName(postgres.getDriverClassName());
		return dataSource;
	}

	private JdbcTemplate jdbcTemplate() {
		return new JdbcTemplate(dataSource());
	}

	private void assertMatchingIndexesCreated() {
		assertThat(jdbcTemplate().queryForObject("""
				select count(*)
				from pg_indexes
				where tablename = 'record_embeddings'
					and indexname in (
						'uk_record_embedding_record_model',
						'idx_record_embeddings_gemini_embedding_2_hvc'
					)
				""", Integer.class)).isEqualTo(2);
	}

	private void assertRunningInsightStatusAllowed() {
		assertThat(jdbcTemplate().queryForObject("""
				select pg_get_constraintdef(oid)
				from pg_constraint
				where conname = 'insights_status_check'
				""", String.class)).contains("RUNNING");
	}

	private void assertRecordAnswersNormalized() {
		assertThat(jdbcTemplate().queryForObject("""
				select count(*)
				from information_schema.columns
				where table_name = 'record_answers'
					and column_name in ('question_description', 'question_text', 'required', 'sort_order')
				""", Integer.class)).isZero();
		assertThat(jdbcTemplate().queryForObject("""
				select count(*)
				from information_schema.table_constraints
				where table_name = 'record_answers'
					and constraint_name = 'fk_record_answers_template_question'
					and constraint_type = 'FOREIGN KEY'
				""", Integer.class)).isEqualTo(1);
	}
}
