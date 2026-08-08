package com.itcotato.dortfolio.domain.template.entity;

import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
	public static final int BUILTIN_CODE_MAX_LENGTH = 50;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "builtin_code", unique = true, length = BUILTIN_CODE_MAX_LENGTH)
	private String builtinCode;

	@Column(name = "builtin_version")
	private Integer builtinVersion;

	@Column(nullable = false, length = TITLE_MAX_LENGTH)
	private String title;

	@Column(length = DESCRIPTION_MAX_LENGTH)
	private String description;

	@Column(nullable = false)
	private boolean isBuiltin;

	@Column
	private LocalDateTime deletedAt;

	@OneToMany(mappedBy = "template", cascade = CascadeType.ALL)
	private final List<TemplateQuestion> questions = new ArrayList<>();

	private Template(User user, String builtinCode, Integer builtinVersion, String title, String description, boolean isBuiltin) {
		this.user = user;
		this.builtinCode = builtinCode;
		this.builtinVersion = builtinVersion;
		this.title = title;
		this.description = description;
		this.isBuiltin = isBuiltin;
	}

	public static Template createCustom(User user, String title, String description) {
		return new Template(user, null, null, title, description, false);
	}

	public static Template createBuiltin(String builtinCode, int builtinVersion, String title, String description) {
		return new Template(null, builtinCode, builtinVersion, title, description, true);
	}

	public void initializeQuestions(List<TemplateQuestion> newQuestions) {
		if (!questions.isEmpty()) {
			throw new IllegalStateException("템플릿 질문은 생성 후 변경할 수 없습니다.");
		}
		newQuestions.stream()
			.sorted(Comparator.comparingInt(TemplateQuestion::getSortOrder))
			.forEach(this::addQuestion);
	}

	private void addQuestion(TemplateQuestion question) {
		question.assignTemplate(this);
		questions.add(question);
	}

	public void delete() {
		this.deletedAt = LocalDateTime.now();
	}

	public boolean isDeleted() {
		return deletedAt != null;
	}

	public java.util.UUID getUserId() {
		return user == null ? null : user.getId();
	}
}
