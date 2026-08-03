package com.itcotato.dortfolio.domain.record.entity;

import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "record_answers",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_record_answer_question", columnNames = {"record_id", "template_question_id"})
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecordAnswer extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "record_id", nullable = false)
	private Record record;

	@Column(name = "template_question_id", nullable = false)
	private UUID templateQuestionId;

	@Column(nullable = false, length = 30)
	private String questionText;

	@Column(length = 100)
	private String questionDescription;

	@Column(nullable = false)
	private boolean required;

	@Column(nullable = false)
	private int sortOrder;

	@Column(nullable = false, columnDefinition = "text")
	private String answerText;

	@Builder
	private RecordAnswer(
		Record record,
		UUID templateQuestionId,
		String questionText,
		String questionDescription,
		boolean required,
		int sortOrder,
		String answerText
	) {
		this.record = record;
		this.templateQuestionId = templateQuestionId;
		this.questionText = questionText;
		this.questionDescription = questionDescription;
		this.required = required;
		this.sortOrder = sortOrder;
		this.answerText = answerText;
	}

	public void updateAnswer(String answerText) {
		this.answerText = answerText;
	}
}
