package com.itcotato.dortfolio.domain.template.entity;

import com.itcotato.dortfolio.domain.activity.entity.Activity;
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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "activity_templates",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_activity_template", columnNames = {"activity_id", "template_id"})
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityTemplate extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "activity_id", nullable = false)
	private Activity activity;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "template_id", nullable = false)
	private Template template;

	@Column(nullable = false)
	private int sortOrder;

	private ActivityTemplate(Activity activity, Template template, int sortOrder) {
		this.activity = activity;
		this.template = template;
		this.sortOrder = sortOrder;
	}

	public static ActivityTemplate create(Activity activity, Template template, int sortOrder) {
		return new ActivityTemplate(activity, template, sortOrder);
	}

	public void updateSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	public UUID getActivityId() {
		return activity.getId();
	}

	public UUID getTemplateId() {
		return template.getId();
	}
}
