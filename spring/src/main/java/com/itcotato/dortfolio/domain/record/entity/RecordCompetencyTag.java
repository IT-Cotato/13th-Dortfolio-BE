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
	name = "record_competency_tags",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_record_competency_tag", columnNames = {"record_id", "competency_tag_id"})
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecordCompetencyTag extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "record_id", nullable = false)
	private Record record;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "competency_tag_id", nullable = false)
	private CompetencyTag competencyTag;

	@Column(nullable = false)
	private float score;

	private RecordCompetencyTag(Record record, CompetencyTag competencyTag, float score) {
		this.record = record;
		this.competencyTag = competencyTag;
		this.score = score;
	}

	public static RecordCompetencyTag create(Record record, CompetencyTag competencyTag, float score) {
		return new RecordCompetencyTag(record, competencyTag, score);
	}
}
