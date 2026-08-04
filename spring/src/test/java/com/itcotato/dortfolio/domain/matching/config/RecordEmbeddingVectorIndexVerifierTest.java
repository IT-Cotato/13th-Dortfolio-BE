package com.itcotato.dortfolio.domain.matching.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class RecordEmbeddingVectorIndexVerifierTest {

	@Mock
	private DataSource dataSource;

	@Mock
	private Connection connection;

	@Mock
	private DatabaseMetaData databaseMetaData;

	@Mock
	private JdbcTemplate jdbcTemplate;

	@Test
	void isAtLeastPgvector08ChecksBoundaryVersions() {
		assertThat(RecordEmbeddingVectorIndexVerifier.isAtLeastPgvector08("0.7.4")).isFalse();
		assertThat(RecordEmbeddingVectorIndexVerifier.isAtLeastPgvector08("0.8.0")).isTrue();
		assertThat(RecordEmbeddingVectorIndexVerifier.isAtLeastPgvector08("0.8.1")).isTrue();
		assertThat(RecordEmbeddingVectorIndexVerifier.isAtLeastPgvector08("1.0.0")).isTrue();
		assertThat(RecordEmbeddingVectorIndexVerifier.isAtLeastPgvector08(null)).isFalse();
		assertThat(RecordEmbeddingVectorIndexVerifier.isAtLeastPgvector08("")).isFalse();
	}

	@Test
	void verifyThrowsWhenPgvectorVersionIsLowerThan08() throws Exception {
		RecordEmbeddingVectorIndexVerifier verifier = verifier();
		givenPostgreSql();
		when(jdbcTemplate.queryForObject(anyString(), eq(String.class))).thenReturn("0.7.4");

		assertThatThrownBy(verifier::verify)
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("pgvector 0.8.0 or later is required");
	}

	@Test
	void verifyThrowsWhenPgvectorVersionCannotBeChecked() throws Exception {
		RecordEmbeddingVectorIndexVerifier verifier = verifier();
		givenPostgreSql();
		when(jdbcTemplate.queryForObject(anyString(), eq(String.class)))
			.thenThrow(new DataAccessResourceFailureException("database unavailable"));

		assertThatThrownBy(verifier::verify)
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Failed to verify pgvector extension version");
	}

	@Test
	void verifyPassesWhenPgvectorVersionIs08OrLater() throws Exception {
		RecordEmbeddingVectorIndexVerifier verifier = verifier();
		givenPostgreSql();
		when(jdbcTemplate.queryForObject(anyString(), eq(String.class))).thenReturn("0.8.0");
		when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any())).thenReturn(1);

		assertThatCode(verifier::verify).doesNotThrowAnyException();
	}

	private void givenPostgreSql() throws Exception {
		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.getMetaData()).thenReturn(databaseMetaData);
		when(databaseMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");
	}

	private RecordEmbeddingVectorIndexVerifier verifier() {
		return new RecordEmbeddingVectorIndexVerifier(
			dataSource,
			jdbcTemplate,
			new MatchingProperties(
				3,
				10,
				10,
				3072,
				List.of("gemini-embedding-2"),
				500,
				30,
				20,
				Duration.ofDays(1),
				ZoneId.of("Asia/Seoul"),
				List.of(new MatchingProperties.QuestionTag("TEST", "테스트 문항"))
			)
		);
	}
}
