package com.itcotato.dortfolio.domain.record.analysis.repository;

import com.itcotato.dortfolio.domain.record.analysis.entity.RecordAnalysis;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecordAnalysisRepository
        extends JpaRepository<RecordAnalysis, UUID> {

    Optional<RecordAnalysis> findByRecord_Id(UUID recordId);

    @Query("""
            select recordAnalysis.record.id
            from RecordAnalysis recordAnalysis
            join recordAnalysis.record record
            where recordAnalysis.lastAttemptFailed = true
              and recordAnalysis.lastFailureRetryable = true
              and record.status =
                  com.itcotato.dortfolio.domain.record.entity.RecordStatus.COMPLETED
              and record.deletedAt is null
              and record.activity.deletedAt is null
              and record.template.deletedAt is null
            """)
    List<UUID> findRetryableRecordIds();

    @Query("""
            select count(recordAnalysis)
            from RecordAnalysis recordAnalysis
            join recordAnalysis.record record
            where record.user.id = :userId
              and record.status =
                  com.itcotato.dortfolio.domain.record.entity.RecordStatus.COMPLETED
              and recordAnalysis.aiAnalysisStatus =
                  com.itcotato.dortfolio.domain.record.analysis.entity.AiAnalysisStatus.COMPLETED
              and record.deletedAt is null
              and record.activity.deletedAt is null
              and record.template.deletedAt is null
            """)
    long countCompletedByUserId(
            @Param("userId") UUID userId
    );

    @Query("""
            select case when count(recordAnalysis) > 0
                        then true
                        else false
                   end
            from RecordAnalysis recordAnalysis
            join recordAnalysis.record record
            where record.user.id = :userId
              and record.status =
                  com.itcotato.dortfolio.domain.record.entity.RecordStatus.COMPLETED
              and recordAnalysis.aiAnalysisStatus =
                  com.itcotato.dortfolio.domain.record.analysis.entity.AiAnalysisStatus.COMPLETED
              and recordAnalysis.analyzedAt > :snapshotAt
              and record.deletedAt is null
              and record.activity.deletedAt is null
              and record.template.deletedAt is null
            """)
    boolean existsCompletedAnalysisAfter(
            @Param("userId") UUID userId,
            @Param("snapshotAt") LocalDateTime snapshotAt
    );

    void deleteByRecord_Id(UUID recordId);
}
