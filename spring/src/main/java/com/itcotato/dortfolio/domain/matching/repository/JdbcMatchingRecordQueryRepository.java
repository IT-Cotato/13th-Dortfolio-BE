package com.itcotato.dortfolio.domain.matching.repository;

import com.itcotato.dortfolio.domain.matching.config.MatchingProperties;
import com.itcotato.dortfolio.domain.matching.model.MatchingRecordCandidate;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
@RequiredArgsConstructor
public class JdbcMatchingRecordQueryRepository implements MatchingRecordQueryRepository {

	private final JdbcTemplate jdbcTemplate;
	private final MatchingProperties matchingProperties;
	private final PlatformTransactionManager transactionManager;

	@Override
	public List<MatchingRecordCandidate> findVectorCandidates(
		UUID userId,
		String embeddingModel,
		float[] questionEmbedding,
		int limit
	) {
		String vectorLiteral = toVectorLiteral(questionEmbedding);
		String sql = buildVectorCandidateSql(embeddingModel);
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.setReadOnly(true);
		return transactionTemplate.execute(status -> {
			jdbcTemplate.execute("set local hnsw.iterative_scan = strict_order");
			return jdbcTemplate.query(sql,
				this::mapCandidate,
				vectorLiteral,
				userId,
				vectorLiteral,
				limit
			);
		});
	}

	String buildVectorCandidateSql(String embeddingModel) {
		String distanceExpression = distanceExpression();
		return """
				select
					r.id as record_id,
					r.title as record_title,
					r.completed_at as record_date,
					a.id as activity_id,
					aty.name as activity_type,
					a.title as activity_title,
					a.started_at as started_at,
					a.ended_at as ended_at,
					a.is_ongoing as is_ongoing,
					t.title as template_title,
					ra.summary as summary,
					greatest(0.0, least(1.0, 1.0 - (%s))) as candidate_score
				from record_embeddings e
				join records r on r.id = e.record_id
				join activity a on a.id = r.activity_id
				join activity_type aty on aty.id = a.activity_type_id
				join templates t on t.id = r.template_id
				join record_analysis ra on ra.record_id = r.id
				where r.user_id = ?
					and r.status = 'COMPLETED'
					and r.deleted_at is null
					and a.deleted_at is null
					and t.deleted_at is null
					and e.embedding_model = %s
					and ra.ai_analysis_status = 'COMPLETED'
					and ra.analyzed_record_updated_at = r.updated_at
					and exists (
						select 1
						from record_answers answer
						where answer.record_id = r.id
							and length(trim(coalesce(answer.answer_text, ''))) > 0
					)
				order by %s
				limit ?
				""".formatted(distanceExpression, toSqlLiteral(embeddingModel), distanceExpression);
	}

	private String distanceExpression() {
		int embeddingDimension = matchingProperties.embeddingDimension();
		return "e.embedding::halfvec(%d) <=> cast(? as halfvec(%d))"
			.formatted(embeddingDimension, embeddingDimension);
	}

	private MatchingRecordCandidate mapCandidate(ResultSet resultSet, int rowNumber) throws SQLException {
		return new MatchingRecordCandidate(
			resultSet.getObject("record_id", UUID.class),
			resultSet.getString("record_title"),
			toLocalDateTime(resultSet, "record_date"),
			resultSet.getObject("activity_id", UUID.class),
			resultSet.getString("activity_type"),
			resultSet.getString("activity_title"),
			toLocalDate(resultSet, "started_at"),
			toLocalDate(resultSet, "ended_at"),
			resultSet.getBoolean("is_ongoing"),
			resultSet.getString("template_title"),
			resultSet.getString("summary"),
			resultSet.getDouble("candidate_score")
		);
	}

	private LocalDateTime toLocalDateTime(ResultSet resultSet, String columnLabel) throws SQLException {
		return resultSet.getTimestamp(columnLabel) == null ? null : resultSet.getTimestamp(columnLabel).toLocalDateTime();
	}

	private LocalDate toLocalDate(ResultSet resultSet, String columnLabel) throws SQLException {
		return resultSet.getDate(columnLabel) == null ? null : resultSet.getDate(columnLabel).toLocalDate();
	}

	private String toVectorLiteral(float[] embedding) {
		StringBuilder builder = new StringBuilder("[");
		for (int index = 0; index < embedding.length; index++) {
			if (index > 0) {
				builder.append(',');
			}
			builder.append(Float.toString(embedding[index]));
		}
		return builder.append(']').toString();
	}

	private String toSqlLiteral(String value) {
		return "'" + value.replace("'", "''") + "'";
	}
}
