package com.itcotato.dortfolio.domain.user.entity;

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
@Table(name = "password_reset_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false)
	private String tokenHash;

	@Column(nullable = false)
	private LocalDateTime expiresAt;

	@Column
	private LocalDateTime usedAt;

	private PasswordResetToken(User user, String tokenHash, LocalDateTime expiresAt) {
		this.user = user;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	public static PasswordResetToken create(User user, String tokenHash, LocalDateTime expiresAt) {
		return new PasswordResetToken(user, tokenHash, expiresAt);
	}

	public void markUsed() {
		this.usedAt = LocalDateTime.now();
	}

	public boolean isUsed() {
		return usedAt != null;
	}
}
