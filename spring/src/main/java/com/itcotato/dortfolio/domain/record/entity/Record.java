package com.itcotato.dortfolio.domain.record.entity;

import com.itcotato.dortfolio.domain.activity.entity.Activity;
import com.itcotato.dortfolio.domain.template.entity.Template;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
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

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RecordStatus status;

	@Column
	private LocalDateTime completedAt;

	@Version
	private Long version;

	@Column
	private LocalDateTime deletedAt;

	@Column
	private LocalDateTime deletePendingUntil;

	@Builder
	private Record(User user, Activity activity, Template template, String title) {
		this.user = user;
		this.activity = activity;
		this.template = template;
		this.title = title;
		this.status = RecordStatus.DRAFT;
	}

	public void updateTitle(String title) {
		this.title = title;
	}

	public void saveDraft() {
		this.status = RecordStatus.DRAFT;
		this.completedAt = null;
	}

	public void complete() {
		if (this.status == RecordStatus.COMPLETED) {
			return;
		}
		this.status = RecordStatus.COMPLETED;
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

	public boolean isDeleted() {
		return deletedAt != null;
	}
}
