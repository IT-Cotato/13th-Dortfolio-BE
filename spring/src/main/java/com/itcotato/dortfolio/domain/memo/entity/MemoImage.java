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

// 기능명세서 3.1.3: 메모에 첨부하는 활동 사진
@Getter
@Entity
@Table(name = "memo_images")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemoImage extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "memo_id", nullable = false)
	private Memo memo;

	@Column(nullable = false)
	private String imageUrl;

	// 이미지 삭제 시 S3 객체를 지우기 위해 Presigned URL 발급 때 받은 키를 함께 저장한다
	@Column
	private String s3Key;

	@Column(nullable = false)
	private int sortOrder;

	private MemoImage(Memo memo, String imageUrl, String s3Key, int sortOrder) {
		this.memo = memo;
		this.imageUrl = imageUrl;
		this.s3Key = s3Key;
		this.sortOrder = sortOrder;
	}

	public static MemoImage create(Memo memo, String imageUrl, String s3Key, int sortOrder) {
		return new MemoImage(memo, imageUrl, s3Key, sortOrder);
	}
}
