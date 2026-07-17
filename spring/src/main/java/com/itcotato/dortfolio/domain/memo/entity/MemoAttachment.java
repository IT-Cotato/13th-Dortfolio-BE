package com.itcotato.dortfolio.domain.memo.entity;

import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "memo_attachments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemoAttachment extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "memo_id", nullable = false)
	private Memo memo;

	@Column(nullable = false)
	private String fileUrl;

	@Column(nullable = false)
	private String fileType;

	@Column(nullable = false)
	private int fileSize;

	@Column(nullable = false)
	private int sortOrder;

	private MemoAttachment(Memo memo, String fileUrl, String fileType, int fileSize, int sortOrder) {
		this.memo = memo;
		this.fileUrl = fileUrl;
		this.fileType = fileType;
		this.fileSize = fileSize;
		this.sortOrder = sortOrder;
	}

	public static MemoAttachment create(Memo memo, String fileUrl, String fileType, int fileSize, int sortOrder) {
		return new MemoAttachment(memo, fileUrl, fileType, fileSize, sortOrder);
	}
}
