package com.itcotato.dortfolio.domain.record.analysis.entity;

import com.itcotato.dortfolio.domain.record.entity.Record;
import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Entity
@Table(
	name = "record_analysis",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_record_analysis_record", columnNames = "record_id")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecordAnalysis extends BaseEntity {

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "record_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Record record;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AiAnalysisStatus aiAnalysisStatus;

	@Column(columnDefinition = "text")
	private String summary;

	@Column(columnDefinition = "text")
	private String evidenceSnippets;

	@Column(columnDefinition = "text")
	private String failureReason;

	@Column(nullable = false)
	private boolean failureRetryable;

	@Column
	private LocalDateTime analyzedAt;

	@Column
	private LocalDateTime analyzedRecordUpdatedAt;

	@Column
	private LocalDateTime lastAttemptedAt;

	@Column(nullable = false)
	private boolean lastAttemptFailed;

	@Column(columnDefinition = "text")
	private String lastFailureReason;

	@Column(nullable = false)
	private boolean lastFailureRetryable;

	private RecordAnalysis(Record record) {
		this.record = record;
		this.aiAnalysisStatus = AiAnalysisStatus.PENDING;
	}

	public static RecordAnalysis pending(Record record) {
		return new RecordAnalysis(record);
	}

	public void markPending() {
		this.aiAnalysisStatus = AiAnalysisStatus.PENDING;
		this.summary = null;
		this.evidenceSnippets = null;
		this.failureReason = null;
		this.failureRetryable = false;
		this.analyzedAt = null;
		this.lastAttemptedAt = LocalDateTime.now();
		this.lastAttemptFailed = false;
		this.lastFailureReason = null;
		this.lastFailureRetryable = false;
	}

	public void complete(String summary, String evidenceSnippets, LocalDateTime recordUpdatedAt) {
		this.aiAnalysisStatus = AiAnalysisStatus.COMPLETED;
		this.summary = summary;
		this.evidenceSnippets = evidenceSnippets;
		this.failureReason = null;
		this.failureRetryable = false;
		this.analyzedAt = LocalDateTime.now();
		this.analyzedRecordUpdatedAt = recordUpdatedAt;
		this.lastAttemptedAt = LocalDateTime.now();
		this.lastAttemptFailed = false;
		this.lastFailureReason = null;
		this.lastFailureRetryable = false;
	}

	public void fail(String failureReason, boolean retryable) {
		this.failureReason = failureReason;
		this.failureRetryable = retryable;
		this.lastAttemptedAt = LocalDateTime.now();
		this.lastAttemptFailed = true;
		this.lastFailureReason = failureReason;
		this.lastFailureRetryable = retryable;
		if (this.aiAnalysisStatus != AiAnalysisStatus.COMPLETED) {
			this.aiAnalysisStatus = AiAnalysisStatus.FAILED;
			this.summary = null;
			this.evidenceSnippets = null;
			this.analyzedAt = null;
			this.analyzedRecordUpdatedAt = null;
		}
	}
}
