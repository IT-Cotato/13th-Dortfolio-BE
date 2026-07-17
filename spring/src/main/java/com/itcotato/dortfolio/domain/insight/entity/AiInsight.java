package com.itcotato.dortfolio.domain.insight.entity;

import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "ai_insights")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiInsight extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false)
	private LocalDate insightDate;

	@Column(nullable = false)
	private String insightType;

	@Lob
	@Column(nullable = false)
	private String content;

	@Column(nullable = false)
	private LocalDateTime refreshedAt;

	private AiInsight(User user, LocalDate insightDate, String insightType, String content, LocalDateTime refreshedAt) {
		this.user = user;
		this.insightDate = insightDate;
		this.insightType = insightType;
		this.content = content;
		this.refreshedAt = refreshedAt;
	}

	public static AiInsight create(User user, LocalDate insightDate, String insightType, String content) {
		return new AiInsight(user, insightDate, insightType, content, LocalDateTime.now());
	}

	public void refresh(String content) {
		this.content = content;
		this.refreshedAt = LocalDateTime.now();
	}
}
