package com.itcotato.dortfolio.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.output.MigrateResult;
import org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension;
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

		assertThat(result.migrationsExecuted).isEqualTo(11);
		assertThat(result.targetSchemaVersion).isEqualTo("11");
		assertMatchingIndexesCreated();
		assertRunningInsightStatusAllowed();
		assertUserOwnedDataCascadesOnDelete();
		assertSinglePrimaryUserJobConstraintCreated();
	}

	private Flyway flyway() {
		FluentConfiguration configuration = Flyway.configure()
			.dataSource(dataSource())
			.locations("classpath:db/migration")
			.baselineOnMigrate(true)
			.baselineVersion(MigrationVersion.fromVersion("0"));
		configuration.getConfigurationExtension(PostgreSQLConfigurationExtension.class)
			.setTransactionalLock(false);
		return configuration.load();
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

	private void assertUserOwnedDataCascadesOnDelete() {
		assertThat(jdbcTemplate().queryForObject("""
				select count(*)
				from pg_constraint
				where confrelid = 'users'::regclass
					and confdeltype = 'c'
				""", Integer.class)).isEqualTo(10);
	}

	private void assertSinglePrimaryUserJobConstraintCreated() {
		assertThat(jdbcTemplate().queryForObject("""
				select count(*)
				from pg_index i
				join pg_class index_class on index_class.oid = i.indexrelid
				join pg_class table_class on table_class.oid = i.indrelid
				join pg_attribute attribute
					on attribute.attrelid = table_class.oid
					and attribute.attnum = i.indkey[0]
				where table_class.relname = 'user_jobs'
					and index_class.relname = 'uq_user_jobs_single_primary'
					and i.indisunique = true
					and attribute.attname = 'user_id'
					and pg_get_expr(i.indpred, i.indrelid) = '(is_primary = true)'
				""", Integer.class)).isEqualTo(1);
	}
}
