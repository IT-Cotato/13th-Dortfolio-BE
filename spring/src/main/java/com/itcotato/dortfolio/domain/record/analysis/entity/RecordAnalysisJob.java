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

	private RecordAnalysisJob(Record record) {
		this.record = record;
		this.status = RecordAnalysisJobStatus.READY;
	}

	public static RecordAnalysisJob ready(Record record) {
		return new RecordAnalysisJob(record);
	}

	public void start() {
		this.status = RecordAnalysisJobStatus.RUNNING;
		this.startedAt = LocalDateTime.now();
		this.attemptCount++;
	}

	public void complete() {
		this.status = RecordAnalysisJobStatus.COMPLETED;
	}

	public void requeue() {
		this.status = RecordAnalysisJobStatus.READY;
		this.startedAt = null;
	}
}
