package com.itcotato.dortfolio.domain.record.entity;

import com.itcotato.dortfolio.domain.template.entity.TemplateQuestion;
import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "template_question_id", nullable = false)
	private TemplateQuestion templateQuestion;

	@JdbcTypeCode(SqlTypes.LONGVARCHAR)
	@Column(nullable = false)
	private String answerText;

	@Builder
	private RecordAnswer(
		Record record,
		TemplateQuestion templateQuestion,
		String answerText
	) {
		this.record = record;
		this.templateQuestion = templateQuestion;
		this.answerText = answerText;
	}

	public UUID getTemplateQuestionId() {
		return templateQuestion.getId();
	}

	public void updateAnswer(String answerText) {
		this.answerText = answerText;
	}
}
