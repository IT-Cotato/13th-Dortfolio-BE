package com.itcotato.dortfolio.domain.matching.config;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecordEmbeddingVectorIndexVerifier {

	private final DataSource dataSource;
	private final JdbcTemplate jdbcTemplate;
	private final MatchingProperties matchingProperties;

	@EventListener(ApplicationReadyEvent.class)
	public void verify() {
		if (!isPostgreSql()) {
			return;
		}
		verifyPgvectorVersion();
		matchingProperties.indexedEmbeddingModels()
			.forEach(this::verifyHalfvecCosineIndex);
	}

	private boolean isPostgreSql() {
		try (Connection connection = dataSource.getConnection()) {
			return connection.getMetaData().getDatabaseProductName().toLowerCase().contains("postgresql");
		} catch (SQLException exception) {
			log.warn("Failed to inspect database product for record embedding index verification.", exception);
			return false;
		}
	}

	private void verifyHalfvecCosineIndex(String embeddingModel) {
		try {
			Integer indexCount = jdbcTemplate.queryForObject("""
					select count(*)
					from pg_indexes
					where schemaname = current_schema()
						and tablename = 'record_embeddings'
						and indexdef like ?
						and indexdef like '%halfvec_cosine_ops%'
						and indexdef like ?
				""",
				Integer.class,
				"%halfvec(" + matchingProperties.embeddingDimension() + ")%",
				"%" + embeddingModel.replace("'", "''") + "%"
			);
			if (indexCount != null && indexCount > 0) {
				return;
			}
			log.warn(
				"Record embedding vector index is missing. embeddingModel={}, embeddingDimension={}. "
					+ "Create the halfvec HNSW index before production traffic.",
				embeddingModel,
				matchingProperties.embeddingDimension()
			);
		} catch (DataAccessException exception) {
			log.warn("Failed to verify record embedding vector index. embeddingModel={}", embeddingModel, exception);
		}
	}

	private void verifyPgvectorVersion() {
		try {
			String version = jdbcTemplate.queryForObject(
				"select extversion from pg_extension where extname = 'vector'",
				String.class
			);
			if (!isAtLeastPgvector08(version)) {
				throw new IllegalStateException(
					"pgvector 0.8.0 or later is required for hnsw.iterative_scan. currentVersion=" + version
				);
			}
		} catch (DataAccessException exception) {
			throw new IllegalStateException("Failed to verify pgvector extension version.", exception);
		}
	}

	static boolean isAtLeastPgvector08(String version) {
		if (version == null || version.isBlank()) {
			return false;
		}
		String[] parts = version.split("\\.");
		int major = parseVersionPart(parts, 0);
		int minor = parseVersionPart(parts, 1);
		return major > 0 || minor >= 8;
	}

	private static int parseVersionPart(String[] parts, int index) {
		if (index >= parts.length) {
			return 0;
		}
		String digits = parts[index].replaceAll("[^0-9].*$", "");
		return digits.isBlank() ? 0 : Integer.parseInt(digits);
	}
}
