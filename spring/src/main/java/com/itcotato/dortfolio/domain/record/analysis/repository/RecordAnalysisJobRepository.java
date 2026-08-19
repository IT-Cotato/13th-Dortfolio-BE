package com.itcotato.dortfolio.domain.record.analysis.repository;

import com.itcotato.dortfolio.domain.record.analysis.entity.RecordAnalysisJob;
import com.itcotato.dortfolio.domain.record.analysis.entity.RecordAnalysisJobStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

public interface RecordAnalysisJobRepository
		extends JpaRepository<RecordAnalysisJob, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<RecordAnalysisJob> findFirstByStatusOrderByCreatedAtAsc(
			RecordAnalysisJobStatus status
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<RecordAnalysisJob> findByRecord_Id(UUID recordId);

	@Query("""
			select job
			from RecordAnalysisJob job
			where job.status = :status
	and job.leaseExpiresAt < :leaseExpiresAt
			""")
	java.util.List<RecordAnalysisJob> findAllRunningBefore(
			@Param("status") RecordAnalysisJobStatus status,
			@Param("leaseExpiresAt") LocalDateTime leaseExpiresAt
	);

	@Modifying
	@Query(value = """
			update record_analysis_jobs set lease_expires_at = :leaseExpiresAt
			where id = :jobId and status = 'RUNNING' and claim_token = :claimToken
			""", nativeQuery = true)
	int renewLease(UUID jobId, UUID claimToken, LocalDateTime leaseExpiresAt);

	@Modifying
	@Query(value = """
			update record_analysis_jobs
			set status = 'COMPLETED', claim_token = null, lease_expires_at = null
			where id = :jobId and status = 'RUNNING' and claim_token = :claimToken
			""", nativeQuery = true)
	int completeClaim(UUID jobId, UUID claimToken);

	@Modifying
	@Query(value = """
			update record_analysis_jobs
			set status = 'READY', started_at = null, claim_token = null, lease_expires_at = null
			where id = :jobId and status = 'RUNNING' and claim_token = :claimToken
			""", nativeQuery = true)
	int requeueClaim(UUID jobId, UUID claimToken);

	@Modifying
	@Query(value = """
			update record_analysis_jobs
			set status = 'READY', started_at = null, claim_token = null, lease_expires_at = null
			where status = 'RUNNING' and lease_expires_at < :now
			""", nativeQuery = true)
	int recoverExpiredClaims(LocalDateTime now);
}
