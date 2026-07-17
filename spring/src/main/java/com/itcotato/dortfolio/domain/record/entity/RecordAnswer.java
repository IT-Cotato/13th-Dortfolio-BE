package com.itcotato.dortfolio.domain.record.entity;

import com.itcotato.dortfolio.domain.template.entity.TemplateQuestion;
import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "record_answers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecordAnswer extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "record_id", nullable = false)
	private Record record;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "template_question_id", nullable = false)
	private TemplateQuestion templateQuestion;

	@Lob
	@Column(nullable = false)
	private String answerText;

	private RecordAnswer(Record record, TemplateQuestion templateQuestion, String answerText) {
		this.record = record;
		this.templateQuestion = templateQuestion;
		this.answerText = answerText;
	}

	public static RecordAnswer create(Record record, TemplateQuestion templateQuestion, String answerText) {
		return new RecordAnswer(record, templateQuestion, answerText);
	}

	public void updateAnswer(String answerText) {
		this.answerText = answerText;
	}
}
