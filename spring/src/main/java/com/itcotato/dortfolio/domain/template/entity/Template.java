package com.itcotato.dortfolio.domain.template.entity;

import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "templates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Template extends BaseEntity {

	public static final int TITLE_MAX_LENGTH = 20;
	public static final int DESCRIPTION_MAX_LENGTH = 50;

	@Column(name = "user_id")
	private UUID userId;

	@Column(nullable = false, length = TITLE_MAX_LENGTH)
	private String title;

	@Column(length = DESCRIPTION_MAX_LENGTH)
	private String description;

	@Column(nullable = false)
	private boolean isDefault;

	@Column(nullable = false)
	private boolean isBuiltin;

	@Column
	private LocalDateTime deletedAt;

	@OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
	private final List<TemplateQuestion> questions = new ArrayList<>();

	private Template(UUID userId, String title, String description, boolean isDefault, boolean isBuiltin) {
		this.userId = userId;
		this.title = title;
		this.description = description;
		this.isDefault = isDefault;
		this.isBuiltin = isBuiltin;
	}

	public static Template createCustom(UUID userId, String title, String description) {
		return new Template(userId, title, description, false, false);
	}

	public static Template createBuiltin(String title, String description) {
		return new Template(null, title, description, true, true);
	}

	public void update(String title, String description) {
		this.title = title;
		this.description = description;
	}

	public void replaceQuestions(List<TemplateQuestion> newQuestions) {
		questions.clear();
		newQuestions.stream()
			.sorted(Comparator.comparingInt(TemplateQuestion::getSortOrder))
			.forEach(this::addQuestion);
	}

	public void addQuestion(TemplateQuestion question) {
		question.assignTemplate(this);
		questions.add(question);
	}

	public void delete() {
		this.deletedAt = LocalDateTime.now();
	}

	public boolean isDeleted() {
		return deletedAt != null;
	}
}
