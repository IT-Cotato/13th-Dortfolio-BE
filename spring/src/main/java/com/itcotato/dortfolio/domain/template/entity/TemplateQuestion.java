package com.itcotato.dortfolio.domain.template.entity;

import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "template_questions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TemplateQuestion extends BaseEntity {

	public static final int QUESTION_TEXT_MAX_LENGTH = 30;
	public static final int DESCRIPTION_MAX_LENGTH = 100;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "template_id", nullable = false)
	private Template template;

	@Column(nullable = false, length = QUESTION_TEXT_MAX_LENGTH)
	private String questionText;

	@Column(length = DESCRIPTION_MAX_LENGTH)
	private String description;

	@Column(nullable = false)
	private boolean required;

	@Column(nullable = false)
	private int sortOrder;

	private TemplateQuestion(String questionText, String description, boolean required, int sortOrder) {
		this.questionText = questionText;
		this.description = description;
		this.required = required;
		this.sortOrder = sortOrder;
	}

	public static TemplateQuestion create(String questionText, String description, boolean required, int sortOrder) {
		return new TemplateQuestion(questionText, description, required, sortOrder);
	}

	void assignTemplate(Template template) {
		this.template = template;
	}
}
