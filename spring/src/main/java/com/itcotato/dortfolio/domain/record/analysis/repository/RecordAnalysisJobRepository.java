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
}
