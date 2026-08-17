package com.itcotato.dortfolio.domain.record.analysis.repository;

import com.itcotato.dortfolio.domain.record.analysis.dto.StrengthMatchCandidate;
import com.itcotato.dortfolio.domain.record.analysis.exception.RecordAnalysisErrorCode;
import com.itcotato.dortfolio.domain.record.entity.StrengthTagEmbedding;
import com.itcotato.dortfolio.global.exception.CustomException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class JdbcStrengthMatchCandidateQuery implements StrengthMatchCandidateQuery {

	private final JdbcTemplate jdbcTemplate;

	@Override
	@Transactional(readOnly = true)
	public List<StrengthMatchCandidate> findTopCandidates(
		String embeddingModel,
		float[] recordEmbedding,
		int limit,
		double minimumSimilarity
	) {
		validateArguments(embeddingModel, recordEmbedding, limit, minimumSimilarity);
		validateCoverage(embeddingModel);

		String vector = toVectorLiteral(recordEmbedding);
		return jdbcTemplate.query(
			"""
			select
				strength_tag.id as strength_tag_id,
				strength_tag.name,
				strength_tag.description,
				strength_tag.evaluation_criteria,
				strength_tag.positive_example,
				strength_tag.negative_example,
				1.0 - (embedding.embedding <=> cast(? as vector)) as cosine_similarity
			from strength_tag_embeddings embedding
			join strength_tags strength_tag
			  on strength_tag.id = embedding.strength_tag_id
			where embedding.embedding_model = ?
			  and 1.0 - (embedding.embedding <=> cast(? as vector)) > ?
			order by
				embedding.embedding <=> cast(? as vector) asc,
				strength_tag.id asc
			limit ?
			""",
			this::mapCandidate,
			vector,
			embeddingModel,
			vector,
			minimumSimilarity,
			vector,
			limit
		);
	}

	private void validateCoverage(String embeddingModel) {
		EmbeddingCoverage coverage = jdbcTemplate.queryForObject(
			"""
			select
				count(strength_tag.id) as total_count,
				count(embedding.id) as embedded_count
			from strength_tags strength_tag
			left join strength_tag_embeddings embedding
			  on embedding.strength_tag_id = strength_tag.id
			 and embedding.embedding_model = ?
			""",
			(resultSet, rowNumber) -> new EmbeddingCoverage(
				resultSet.getLong("total_count"),
				resultSet.getLong("embedded_count")
			),
			embeddingModel
		);

		if (coverage == null
			|| coverage.totalCount() == 0
			|| coverage.totalCount() != coverage.embeddedCount()) {
			throw new CustomException(RecordAnalysisErrorCode.STRENGTH_TAG_EMBEDDING_NOT_READY);
		}
	}

	private StrengthMatchCandidate mapCandidate(ResultSet resultSet, int rowNumber) throws SQLException {
		return new StrengthMatchCandidate(
			resultSet.getObject("strength_tag_id", UUID.class),
			resultSet.getString("name"),
			resultSet.getString("description"),
			resultSet.getString("evaluation_criteria"),
			resultSet.getString("positive_example"),
			resultSet.getString("negative_example"),
			resultSet.getFloat("cosine_similarity")
		);
	}

	private void validateArguments(
		String embeddingModel,
		float[] recordEmbedding,
		int limit,
		double minimumSimilarity
	) {
		if (!StringUtils.hasText(embeddingModel)
			|| recordEmbedding == null
			|| recordEmbedding.length != StrengthTagEmbedding.EMBEDDING_DIMENSION
			|| limit < 1
			|| !Double.isFinite(minimumSimilarity)
			|| minimumSimilarity < -1.0
			|| minimumSimilarity > 1.0) {
			throw new IllegalArgumentException("Invalid strength match candidate arguments");
		}

		boolean hasMagnitude = false;
		for (float value : recordEmbedding) {
			if (!Float.isFinite(value)) {
				throw new IllegalArgumentException("Record embedding contains a non-finite value");
			}
			hasMagnitude |= value != 0.0f;
		}
		if (!hasMagnitude) {
			throw new IllegalArgumentException("Record embedding must have a non-zero magnitude");
		}
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

	private record EmbeddingCoverage(long totalCount, long embeddedCount) {
	}
}
