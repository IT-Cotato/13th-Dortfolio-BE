package com.itcotato.dortfolio.domain.record.entity;

import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
	name = "strength_tag_embeddings",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_strength_tag_embedding",
			columnNames = {"strength_tag_id", "embedding_model"}
		)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StrengthTagEmbedding extends BaseEntity {

	public static final int EMBEDDING_DIMENSION = 3072;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "strength_tag_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private StrengthTag strengthTag;

	@Column(name = "embedding_model", nullable = false, length = 100)
	private String embeddingModel;

	@JdbcTypeCode(SqlTypes.VECTOR)
	@Column(name = "embedding", nullable = false, columnDefinition = "vector(3072)")
	private float[] embedding;

	private StrengthTagEmbedding(
		StrengthTag strengthTag,
		String embeddingModel,
		float[] embedding
	) {
		validate(embeddingModel, embedding);
		this.strengthTag = strengthTag;
		this.embeddingModel = embeddingModel;
		this.embedding = embedding.clone();
	}

	public static StrengthTagEmbedding create(
		StrengthTag strengthTag,
		String embeddingModel,
		float[] embedding
	) {
		return new StrengthTagEmbedding(strengthTag, embeddingModel, embedding);
	}

	private static void validate(String embeddingModel, float[] embedding) {
		if (embeddingModel == null || embeddingModel.isBlank()) {
			throw new IllegalArgumentException("임베딩 모델명은 비어 있을 수 없습니다.");
		}
		if (embedding == null || embedding.length != EMBEDDING_DIMENSION) {
			throw new IllegalArgumentException("임베딩 벡터는 3072차원이어야 합니다.");
		}
		for (float value : embedding) {
			if (!Float.isFinite(value)) {
				throw new IllegalArgumentException("임베딩 벡터에 유효하지 않은 값이 포함되어 있습니다.");
			}
		}
	}
}
