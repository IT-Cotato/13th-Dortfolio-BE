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

	// 기능명세서상 활동 태그는 선택 입력이라 nullable
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "activity_id")
	private Activity activity;

	// 기능명세서상 제목은 선택 입력이라 nullable
	@Column
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
	private LocalDateTime expiresAt;

	private static final int EXPIRE_AFTER_DAYS = 30;

	private Memo(User user, Activity activity, String title, String content, String color, int sortOrder) {
		this.user = user;
		this.activity = activity;
		this.title = title;
		this.content = content;
		this.color = color;
		this.sortOrder = sortOrder;
		this.isImportant = false;
		this.useCount = 0;
		this.expiresAt = LocalDateTime.now().plusDays(EXPIRE_AFTER_DAYS);
	}

	public static Memo create(User user, Activity activity, String title, String content, String color, int sortOrder) {
		return new Memo(user, activity, title, content, color, sortOrder);
	}

	// 기능명세서 3.5.4: 메모 수정 시 제목/내용만 변경 가능, 활동 태그는 수정/추가 불가
	public void update(String title, String content, String color) {
		this.title = title;
		this.content = content;
		this.color = color;
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

	public boolean isDeleted() {
		return deletedAt != null;
	}
}
