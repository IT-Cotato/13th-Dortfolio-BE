package com.itcotato.dortfolio.domain.template.entity;

import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

	@Column(name = "activity_id", nullable = false)
	private UUID activityId;

	@Column(name = "template_id", nullable = false)
	private UUID templateId;

	@Column(nullable = false)
	private int sortOrder;

	private ActivityTemplate(UUID activityId, UUID templateId, int sortOrder) {
		this.activityId = activityId;
		this.templateId = templateId;
		this.sortOrder = sortOrder;
	}

	public static ActivityTemplate create(UUID activityId, UUID templateId, int sortOrder) {
		return new ActivityTemplate(activityId, templateId, sortOrder);
	}
}
