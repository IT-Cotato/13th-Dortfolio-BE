package com.itcotato.dortfolio.domain.record.analysis.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.itcotato.dortfolio.domain.record.analysis.dto.StrengthMatchCandidate;
import com.itcotato.dortfolio.domain.record.analysis.exception.RecordAnalysisErrorCode;
import com.itcotato.dortfolio.domain.record.entity.StrengthTagEmbedding;
import com.itcotato.dortfolio.global.exception.CustomException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class JdbcStrengthMatchCandidateQueryContainerTest {

	private static final String MODEL = "gemini-embedding-2";

	@Container
	private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
		DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres")
	);

	private static JdbcTemplate jdbcTemplate;
	private static JdbcStrengthMatchCandidateQuery candidateQuery;
	private static List<UUID> strengthTagIds;

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
		candidateQuery = new JdbcStrengthMatchCandidateQuery(jdbcTemplate);
		strengthTagIds = jdbcTemplate.queryForList(
			"select id from strength_tags order by id",
			UUID.class
		);
	}

	@Test
	void excludesCandidatesBelowMinimumSimilarity() {
		insertEmbeddings();

		List<StrengthMatchCandidate> result = candidateQuery.findTopCandidates(
			MODEL,
			vectorValues(1.0, 0.0),
			5,
			0.4
		);

		assertThat(result)
			.singleElement()
			.satisfies(candidate -> {
				assertThat(candidate.strengthTagId()).isEqualTo(strengthTagIds.get(0));
				assertThat(candidate.cosineSimilarity()).isEqualTo(1.0f);
			});
	}

	@AfterEach
	void tearDown() {
		jdbcTemplate.update("delete from strength_tag_embeddings");
	}

	@Test
	void rejectsMatchingWhenEmbeddingCoverageIsIncomplete() {
		insertEmbeddings();
		jdbcTemplate.update(
			"delete from strength_tag_embeddings where strength_tag_id = ?",
			strengthTagIds.get(0)
		);

		assertThatThrownBy(() -> candidateQuery.findTopCandidates(
			MODEL,
			vectorValues(1.0, 0.0),
			5,
			0.4
		))
			.isInstanceOfSatisfying(CustomException.class, exception ->
				assertThat(exception.getErrorCode())
					.isEqualTo(RecordAnalysisErrorCode.STRENGTH_TAG_EMBEDDING_NOT_READY)
			);
	}

	private static void insertEmbeddings() {
		jdbcTemplate.update("delete from strength_tag_embeddings");
		for (int index = 0; index < strengthTagIds.size(); index++) {
			String vector = switch (index) {
				case 0 -> vectorLiteral(1.0, 0.0);
				case 1 -> vectorLiteral(0.39, Math.sqrt(1.0 - 0.39 * 0.39));
				default -> vectorLiteral(0.0, 1.0);
			};
			jdbcTemplate.update("""
				insert into strength_tag_embeddings (
					id, created_at, updated_at, strength_tag_id, embedding_model, embedding
				) values (?, now(), now(), ?, ?, ?::vector)
				""", UUID.randomUUID(), strengthTagIds.get(index), MODEL, vector);
		}
	}

	private static float[] vectorValues(double first, double second) {
		float[] values = new float[StrengthTagEmbedding.EMBEDDING_DIMENSION];
		values[0] = (float) first;
		values[1] = (float) second;
		return values;
	}

	private static String vectorLiteral(double first, double second) {
		List<String> values = new ArrayList<>(Collections.nCopies(
			StrengthTagEmbedding.EMBEDDING_DIMENSION,
			"0"
		));
		values.set(0, Double.toString(first));
		values.set(1, Double.toString(second));
		return "[" + String.join(",", values) + "]";
	}
}
