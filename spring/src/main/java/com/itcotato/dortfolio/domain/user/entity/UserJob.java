package com.itcotato.dortfolio.domain.user.entity;

import com.itcotato.dortfolio.domain.job.entity.Job;
import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "user_jobs",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_user_job", columnNames = {"user_id", "job_id"})
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserJob extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "job_id", nullable = false)
	private Job job;

	@Column(nullable = false)
	private boolean isPrimary;

	private UserJob(User user, Job job, boolean isPrimary) {
		this.user = user;
		this.job = job;
		this.isPrimary = isPrimary;
	}

	public static UserJob create(User user, Job job, boolean isPrimary) {
		return new UserJob(user, job, isPrimary);
	}

	public void changePrimary(boolean isPrimary) {
		this.isPrimary = isPrimary;
	}
}
