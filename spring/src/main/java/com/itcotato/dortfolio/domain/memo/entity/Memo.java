package com.itcotato.dortfolio.domain.memo.entity;

import com.itcotato.dortfolio.domain.activity.entity.Activity;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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

	@JdbcTypeCode(SqlTypes.LONGVARCHAR)
	@Column(nullable = false)
	private String content;

	@Column(nullable = false)
	private boolean isImportant;

	@Column(nullable = false)
	private int useCount;

	@Column(nullable = false)
	private int sortOrder;

	// [사용되지 않는 상태] 메모 삭제는 복구 불가(기능명세서 3.2.3.1.1)라 하드 삭제이고,
	// 기록에 연결된 메모는 아예 삭제를 막으므로(M004) 이 소프트 삭제 상태는 설정되지 않는다.
	// Record 도메인의 RecordMemoService#hasDeletedMemo가 아직 참조 중이라 남겨둔 상태.
	// TODO: Record 도메인 정리와 함께 제거 예정 (PR #28 리뷰 합의)
	@Column
	private LocalDateTime deletedAt;

	@Column
	private LocalDateTime deletePendingUntil;

	@Column
	private LocalDateTime expiresAt;

	private static final int EXPIRE_AFTER_DAYS = 30;

	private Memo(User user, Activity activity, String title, String content, int sortOrder) {
		this.user = user;
		this.activity = activity;
		this.title = title;
		this.content = content;
		this.sortOrder = sortOrder;
		this.isImportant = false;
		this.useCount = 0;
		this.expiresAt = LocalDateTime.now().plusDays(EXPIRE_AFTER_DAYS);
	}

	public static Memo create(User user, Activity activity, String title, String content, int sortOrder) {
		return new Memo(user, activity, title, content, sortOrder);
	}

	// 기능명세서 3.5.4: 메모 수정 시 제목/내용만 변경 가능, 활동 태그는 수정/추가 불가
	public void update(String title, String content) {
		this.title = title;
		this.content = content;
	}

	public void markImportant(boolean important) {
		this.isImportant = important;
	}

	// [사용되지 않는 메서드] 메모 삭제 API는 하드 삭제라 호출하지 않는다.
	// RecordServiceTest가 아직 호출 중이라 제거하지 못한 상태.
	// TODO: Record 도메인 정리와 함께 제거 예정 (PR #28 리뷰 합의)
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
