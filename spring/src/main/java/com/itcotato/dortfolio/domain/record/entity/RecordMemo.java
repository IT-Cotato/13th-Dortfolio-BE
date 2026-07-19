package com.itcotato.dortfolio.domain.record.entity;

import com.itcotato.dortfolio.domain.memo.entity.Memo;
import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "record_memos",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_record_memo", columnNames = {"record_id", "memo_id"})
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecordMemo extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "record_id", nullable = false)
	private Record record;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "memo_id", nullable = false)
	private Memo memo;

	@Column(nullable = false)
	private int sortOrder;

	@Column(nullable = false)
	private boolean isCollapsed;

	@Builder
	private RecordMemo(Record record, Memo memo, int sortOrder, boolean isCollapsed) {
		this.record = record;
		this.memo = memo;
		this.sortOrder = sortOrder;
		this.isCollapsed = isCollapsed;
	}

	public void updateSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	public void changeCollapsed(boolean collapsed) {
		this.isCollapsed = collapsed;
	}
}
