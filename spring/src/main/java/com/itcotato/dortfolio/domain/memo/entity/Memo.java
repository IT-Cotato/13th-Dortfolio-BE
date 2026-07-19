package com.itcotato.dortfolio.domain.memo.entity;

import com.itcotato.dortfolio.domain.activity.entity.Activity;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "memos")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Memo extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "activity_id", nullable = false)
	private Activity activity;

	@Column(nullable = false)
	private String title;

	@Lob
	@Column(nullable = false)
	private String content;

	@Column
	private String color;

	@Column(nullable = false)
	private boolean isImportant;

	@Column(nullable = false)
	private int useCount;

	@Column(nullable = false)
	private int sortOrder;

	@Column
	private LocalDateTime deletedAt;

	@Column
	private LocalDateTime deletePendingUntil;

	@Column
	private LocalDateTime expiresAt;

	private Memo(User user, Activity activity, String title, String content, String color, int sortOrder) {
		this.user = user;
		this.activity = activity;
		this.title = title;
		this.content = content;
		this.color = color;
		this.sortOrder = sortOrder;
		this.isImportant = false;
		this.useCount = 0;
	}

	public static Memo create(User user, Activity activity, String title, String content, String color, int sortOrder) {
		return new Memo(user, activity, title, content, color, sortOrder);
	}

	public void markImportant(boolean important) {
		this.isImportant = important;
	}

	public void increaseUseCount() {
		this.useCount++;
	}

	public void decreaseUseCount() {
		if (this.useCount > 0) {
			this.useCount--;
		}
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
