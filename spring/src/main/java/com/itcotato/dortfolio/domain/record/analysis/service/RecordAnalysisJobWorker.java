package com.itcotato.dortfolio.domain.record.analysis.service;

import com.itcotato.dortfolio.domain.record.analysis.entity.RecordAnalysisJob;
import com.itcotato.dortfolio.domain.record.analysis.entity.RecordAnalysisJobStatus;
import com.itcotato.dortfolio.domain.record.analysis.config.RecordAnalysisProperties;
import com.itcotato.dortfolio.domain.record.analysis.repository.RecordAnalysisJobRepository;
import com.itcotato.dortfolio.domain.record.analysis.repository.RecordAnalysisRepository;
import com.itcotato.dortfolio.domain.record.analysis.entity.AiAnalysisStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecordAnalysisJobWorker {

	private final RecordAnalysisJobRepository jobRepository;
	private final RecordAnalysisRepository recordAnalysisRepository;
	private final RecordAnalysisService recordAnalysisService;
	private final TransactionTemplate transactionTemplate;
	private final Clock clock;
	private final RecordAnalysisProperties properties;
	private final TaskScheduler taskScheduler;

	@Async("recordAnalysisTaskExecutor")
	@Scheduled(fixedDelayString = "${record-analysis.job-poll-interval}")
	public void processNext() {
		Optional<JobTarget> target = transactionTemplate.execute(status -> jobRepository
			.findFirstByStatusOrderByCreatedAtAsc(RecordAnalysisJobStatus.READY)
			.map(job -> {
				LocalDateTime now = LocalDateTime.now(clock);
				UUID claimToken = job.start(now, now.plus(properties.jobStaleRunningTimeout()));
				return new JobTarget(job.getId(), job.getRecord().getId(), claimToken,
					job.getAnalysisGeneration());
			}));

		if (target == null || target.isEmpty()) {
			return;
		}

		JobTarget jobTarget = target.get();
		ScheduledFuture<?> heartbeat = taskScheduler.scheduleAtFixedRate(
			() -> renewLease(jobTarget),
		heartbeatInterval()
		);
		try {
			if (shouldAnalyze(jobTarget.recordId(), jobTarget.analysisGeneration())) {
				recordAnalysisService.analyze(jobTarget.recordId(), jobTarget.analysisGeneration(),
					jobTarget.jobId(), jobTarget.claimToken());
			}
			transactionTemplate.executeWithoutResult(status -> jobRepository
				.completeClaim(jobTarget.jobId(), jobTarget.claimToken()));
		} catch (RuntimeException exception) {
			log.error("Record analysis job execution failed. jobId={}, recordId={}",
				jobTarget.jobId(), jobTarget.recordId(), exception);
			transactionTemplate.executeWithoutResult(status -> jobRepository
				.requeueClaim(jobTarget.jobId(), jobTarget.claimToken()));
		} finally {
			heartbeat.cancel(false);
		}
	}

	private void renewLease(JobTarget jobTarget) {
		transactionTemplate.executeWithoutResult(status -> jobRepository
			.renewLease(jobTarget.jobId(), jobTarget.claimToken(),
				LocalDateTime.now(clock).plus(properties.jobStaleRunningTimeout())));
	}

	private Duration heartbeatInterval() {
		long seconds = Math.max(1, properties.jobStaleRunningTimeout().toSeconds() / 3);
		return Duration.ofSeconds(seconds);
	}

	private boolean shouldAnalyze(UUID recordId, long generation) {
		return recordAnalysisRepository.findByRecord_Id(recordId)
			.map(analysis -> analysis.getAiAnalysisStatus() == AiAnalysisStatus.PENDING
				&& analysis.isCurrentGeneration(generation))
			.orElse(false);
	}

	@Scheduled(fixedDelayString = "${record-analysis.job-recovery-interval}")
	public void recoverStaleRunningJobs() {
		transactionTemplate.executeWithoutResult(status -> {
			LocalDateTime now = LocalDateTime.now(clock);
			jobRepository.recoverExpiredClaims(now);
		});
	}

	private record JobTarget(UUID jobId, UUID recordId, UUID claimToken, long analysisGeneration) {
	}
}
