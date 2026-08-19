package com.itcotato.dortfolio.domain.record.analysis.service;

import com.itcotato.dortfolio.domain.record.analysis.entity.RecordAnalysisJob;
import com.itcotato.dortfolio.domain.record.analysis.entity.RecordAnalysisJobStatus;
import com.itcotato.dortfolio.domain.record.analysis.config.RecordAnalysisProperties;
import com.itcotato.dortfolio.domain.record.analysis.repository.RecordAnalysisJobRepository;
import com.itcotato.dortfolio.domain.record.analysis.repository.RecordAnalysisRepository;
import com.itcotato.dortfolio.domain.record.analysis.entity.AiAnalysisStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Async;
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

	@Async("recordAnalysisTaskExecutor")
	@Scheduled(fixedDelayString = "${record-analysis.job-poll-interval}")
	public void processNext() {
		Optional<JobTarget> target = transactionTemplate.execute(status -> jobRepository
			.findFirstByStatusOrderByCreatedAtAsc(RecordAnalysisJobStatus.READY)
			.map(job -> {
				job.start();
				return new JobTarget(job.getId(), job.getRecord().getId());
			}));

		if (target == null || target.isEmpty()) {
			return;
		}

		JobTarget jobTarget = target.get();
		try {
			if (shouldAnalyze(jobTarget.recordId())) {
				recordAnalysisService.analyze(jobTarget.recordId());
			}
			transactionTemplate.executeWithoutResult(status -> jobRepository
				.findById(jobTarget.jobId())
				.ifPresent(RecordAnalysisJob::complete));
		} catch (RuntimeException exception) {
			log.error("Record analysis job execution failed. jobId={}, recordId={}",
				jobTarget.jobId(), jobTarget.recordId(), exception);
			transactionTemplate.executeWithoutResult(status -> jobRepository
				.findById(jobTarget.jobId())
				.ifPresent(RecordAnalysisJob::requeue));
		}
	}

	private boolean shouldAnalyze(UUID recordId) {
		return recordAnalysisRepository.findByRecord_Id(recordId)
			.map(analysis -> analysis.getAiAnalysisStatus() == AiAnalysisStatus.PENDING)
			.orElse(false);
	}

	@Scheduled(fixedDelayString = "${record-analysis.job-recovery-interval}")
	public void recoverStaleRunningJobs() {
		transactionTemplate.executeWithoutResult(status -> {
			LocalDateTime startedBefore = LocalDateTime.now(clock)
				.minus(properties.jobStaleRunningTimeout());
			jobRepository.findAllRunningBefore(RecordAnalysisJobStatus.RUNNING, startedBefore)
				.forEach(RecordAnalysisJob::requeue);
		});
	}

	private record JobTarget(UUID jobId, UUID recordId) {
	}
}
