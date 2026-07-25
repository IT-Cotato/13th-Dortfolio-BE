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

	// 메모 자체 삭제는 복구 불가(기능명세서 3.2.3.1.1)라 하드 삭제하므로 메모 도메인에서는 사용하지 않는다.
	// Record 도메인이 기록 복구 검증(RecordMemoService#hasDeletedMemo)에 이 상태를 참조하고 있어 유지한다.
	@Column
	private LocalDateTime deletedAt;

	@Column
	private LocalDateTime deletePendingUntil;

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

	// 메모 삭제 API는 하드 삭제라 이 메서드를 쓰지 않는다.
	// Record 도메인의 기록 복구 검증에서 참조하는 소프트 삭제 표시용으로만 남겨둔 상태.
	// TODO: 메모 하드 삭제와 RecordMemo FK 정책 확정 후 정리 필요
	public void markDeleted(int gracePeriodDays) {
		this.deletedAt = LocalDateTime.now();
		this.deletePendingUntil = LocalDateTime.now().plusDays(gracePeriodDays);
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
