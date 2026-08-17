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

@Getter
@Entity
@Table(
	name = "record_strength_tags",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_record_strength_tag", columnNames = {"record_id", "strength_tag_id"})
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecordStrengthTag extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "record_id", nullable = false)
	private Record record;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "strength_tag_id", nullable = false)
	private StrengthTag strengthTag;

	@Column(nullable = false)
	private float score;

	private RecordStrengthTag(Record record, StrengthTag strengthTag, float score) {
		this.record = record;
		this.strengthTag = strengthTag;
		this.score = score;
	}

	public static RecordStrengthTag create(Record record, StrengthTag strengthTag, float score) {
		return new RecordStrengthTag(record, strengthTag, score);
	}
}
