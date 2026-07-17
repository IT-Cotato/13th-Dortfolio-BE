package com.itcotato.dortfolio.domain.record.entity;

import com.itcotato.dortfolio.activity.entity.Activity;
import com.itcotato.dortfolio.domain.template.entity.Template;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Record extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "activity_id", nullable = false)
	private Activity activity;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "template_id", nullable = false)
	private Template template;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private String status;

	@Column
	private LocalDateTime completedAt;

	@Column
	private LocalDateTime deletedAt;

	@Column
	private LocalDateTime deletePendingUntil;

	private Record(User user, Activity activity, Template template, String title, String status) {
		this.user = user;
		this.activity = activity;
		this.template = template;
		this.title = title;
		this.status = status;
	}

	public static Record create(User user, Activity activity, Template template, String title, String status) {
		return new Record(user, activity, template, title, status);
	}

	public void complete() {
		this.status = "COMPLETED";
		this.completedAt = LocalDateTime.now();
	}

	public void markDeleted(int gracePeriodDays) {
		this.deletedAt = LocalDateTime.now();
		this.deletePendingUntil = LocalDateTime.now().plusDays(gracePeriodDays);
	}

	public void restore() {
		this.deletedAt = null;
		this.deletePendingUntil = null;
	}
}
