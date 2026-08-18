package com.itcotato.dortfolio.domain.record.entity;

import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "strength_tags")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StrengthTag extends BaseEntity {

	@Column(nullable = false, unique = true, length = 50)
	private String code;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 255)
	private String description;

	@Column(name = "evaluation_criteria", nullable = false, length = 255)
	private String evaluationCriteria;

	@Column(name = "positive_example", nullable = false, length = 500)
	private String positiveExample;

	@Column(name = "negative_example", nullable = false, length = 500)
	private String negativeExample;

	private StrengthTag(
		String code,
		String name,
		String description,
		String evaluationCriteria,
		String positiveExample,
		String negativeExample
	) {
		this.code = code;
		this.name = name;
		this.description = description;
		this.evaluationCriteria = evaluationCriteria;
		this.positiveExample = positiveExample;
		this.negativeExample = negativeExample;
	}

	public static StrengthTag create(
		String code,
		String name,
		String description,
		String evaluationCriteria,
		String positiveExample,
		String negativeExample
	) {
		return new StrengthTag(
			code,
			name,
			description,
			evaluationCriteria,
			positiveExample,
			negativeExample
		);
	}
}
