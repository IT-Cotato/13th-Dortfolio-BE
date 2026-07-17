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
@Table(name = "user_term_agreements")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTermAgreement extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false)
	private String termType;

	@Column(nullable = false)
	private boolean agreed;

	@Column(nullable = false)
	private LocalDateTime agreedAt;

	private UserTermAgreement(User user, String termType, boolean agreed, LocalDateTime agreedAt) {
		this.user = user;
		this.termType = termType;
		this.agreed = agreed;
		this.agreedAt = agreedAt;
	}

	public static UserTermAgreement create(User user, String termType, boolean agreed) {
		return new UserTermAgreement(user, termType, agreed, LocalDateTime.now());
	}
}
