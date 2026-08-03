package com.itcotato.dortfolio.domain.record.entity;

import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
	name = "record_embeddings",
	indexes = {
		@Index(name = "uk_record_embedding_record_model", columnList = "record_id, embedding_model", unique = true)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecordEmbedding extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "record_id", nullable = false)
	private Record record;

	@Column(nullable = false)
	private String embeddingModel;

	@JdbcTypeCode(SqlTypes.VECTOR)
	@Column(nullable = false, columnDefinition = "vector")
	private float[] embedding;

	private RecordEmbedding(Record record, String embeddingModel, float[] embedding) {
		this.record = record;
		this.embeddingModel = embeddingModel;
		this.embedding = embedding;
	}

	public static RecordEmbedding create(Record record, String embeddingModel, float[] embedding) {
		return new RecordEmbedding(record, embeddingModel, embedding);
	}
}
