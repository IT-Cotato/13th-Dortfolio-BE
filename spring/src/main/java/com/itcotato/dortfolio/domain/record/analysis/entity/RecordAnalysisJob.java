package com.itcotato.dortfolio.domain.record.analysis.entity;

import com.itcotato.dortfolio.domain.record.entity.Record;
import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "record_analysis_jobs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecordAnalysisJob extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "record_id", nullable = false)
	private Record record;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RecordAnalysisJobStatus status;

	@Column(nullable = false)
	private int attemptCount;

	@Column
	private LocalDateTime startedAt;

	@Column
	private UUID claimToken;

	@Column
	private LocalDateTime leaseExpiresAt;

	private RecordAnalysisJob(Record record) {
		this.record = record;
		this.status = RecordAnalysisJobStatus.READY;
	}

	public static RecordAnalysisJob ready(Record record) {
		return new RecordAnalysisJob(record);
	}

	public UUID start(LocalDateTime now, LocalDateTime leaseExpiresAt) {
		this.status = RecordAnalysisJobStatus.RUNNING;
		this.startedAt = now;
		this.claimToken = UUID.randomUUID();
		this.leaseExpiresAt = leaseExpiresAt;
		this.attemptCount++;
		return claimToken;
	}

	public boolean complete(UUID claimToken) {
		if (!isClaimedBy(claimToken)) {
			return false;
		}
		this.status = RecordAnalysisJobStatus.COMPLETED;
		this.claimToken = null;
		this.leaseExpiresAt = null;
		return true;
	}

	public boolean renewLease(UUID claimToken, LocalDateTime leaseExpiresAt) {
		if (!isClaimedBy(claimToken)) {
			return false;
		}
		this.leaseExpiresAt = leaseExpiresAt;
		return true;
	}

	public boolean requeue(UUID claimToken) {
		if (!isClaimedBy(claimToken)) {
			return false;
		}
		this.status = RecordAnalysisJobStatus.READY;
		this.startedAt = null;
		this.claimToken = null;
		this.leaseExpiresAt = null;
		return true;
	}

	public void reschedule() {
		this.status = RecordAnalysisJobStatus.READY;
		this.startedAt = null;
		this.claimToken = null;
		this.leaseExpiresAt = null;
	}

	private boolean isClaimedBy(UUID token) {
		return status == RecordAnalysisJobStatus.RUNNING
			&& claimToken != null
			&& claimToken.equals(token);
	}
}
