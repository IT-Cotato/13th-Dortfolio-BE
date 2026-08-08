package com.itcotato.dortfolio.domain.insight.repository;

import com.itcotato.dortfolio.domain.record.analysis.entity.RecordAnalysis;
import com.itcotato.dortfolio.domain.record.analysis.entity.AiAnalysisStatus;
import com.itcotato.dortfolio.domain.record.entity.RecordCompetencyTag;
import com.itcotato.dortfolio.domain.insight.config.InsightProperties;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class InsightRecordQueryRepository {

    private static final String ELIGIBLE_CONDITION = """
            record.user.id = :userId
                and record.status =
                    com.itcotato.dortfolio.domain.record.entity.RecordStatus.COMPLETED
                and recordAnalysis.aiAnalysisStatus =
                    com.itcotato.dortfolio.domain.record.analysis.entity.AiAnalysisStatus.COMPLETED
                and record.deletedAt is null
                and record.activity.deletedAt is null
                and recordAnalysis.analyzedRecordUpdatedAt = record.updatedAt
                and exists (
                    select recordEmbedding.id
                    from RecordEmbedding recordEmbedding
                    where recordEmbedding.record = record
                      and recordEmbedding.embeddingModel = :embeddingModel
                )
            """;

    private final EntityManager entityManager;
    private final InsightProperties insightProperties;

    public InsightRecordQueryRepository(
            EntityManager entityManager,
            InsightProperties insightProperties
    ) {
        this.entityManager = entityManager;
        this.insightProperties = insightProperties;
    }

    public long countEligibleRecords(UUID userId) {
        return entityManager.createQuery("""
                        select count(recordAnalysis)
                        from RecordAnalysis recordAnalysis
                        join recordAnalysis.record record
                        where
                        """ + ELIGIBLE_CONDITION, Long.class)
                .setParameter("userId", userId)
                .setParameter("embeddingModel", insightProperties.embeddingModel())
                .getSingleResult();
    }

    public long countEligibleRecordsAt(
            UUID userId,
            LocalDateTime snapshotAt
    ) {
        return entityManager.createQuery("""
                        select count(recordAnalysis)
                        from RecordAnalysis recordAnalysis
                        join recordAnalysis.record record
                        where
                        """ + ELIGIBLE_CONDITION + """
                        and record.completedAt <= :snapshotAt
                        and record.updatedAt <= :snapshotAt
                        and recordAnalysis.analyzedAt <= :snapshotAt
                        """, Long.class)
                .setParameter("userId", userId)
                .setParameter("embeddingModel", insightProperties.embeddingModel())
                .setParameter("snapshotAt", snapshotAt)
                .getSingleResult();
    }

    public boolean existsEligibleRecordAnalyzedAfter(
            UUID userId,
            LocalDateTime snapshotAt
    ) {
        List<UUID> result = entityManager.createQuery("""
                        select recordAnalysis.id
                        from RecordAnalysis recordAnalysis
                        join recordAnalysis.record record
                        where
                        """ + ELIGIBLE_CONDITION + """
                        and recordAnalysis.analyzedAt > :snapshotAt
                        order by recordAnalysis.analyzedAt asc,
                                 recordAnalysis.id asc
                        """, UUID.class)
                .setParameter("userId", userId)
                .setParameter("embeddingModel", insightProperties.embeddingModel())
                .setParameter("snapshotAt", snapshotAt)
                .setMaxResults(1)
                .getResultList();

        return !result.isEmpty();
    }

    public boolean existsEligibleRecordAnalyzedBetween(
            UUID userId,
            LocalDateTime analyzedAfter,
            LocalDateTime snapshotAt
    ) {
        List<UUID> result = entityManager.createQuery("""
                        select recordAnalysis.id
                        from RecordAnalysis recordAnalysis
                        join recordAnalysis.record record
                        where
                        """ + ELIGIBLE_CONDITION + """
                        and record.completedAt <= :snapshotAt
                        and record.updatedAt <= :snapshotAt
                        and recordAnalysis.analyzedAt > :analyzedAfter
                        and recordAnalysis.analyzedAt <= :snapshotAt
                        order by recordAnalysis.analyzedAt asc,
                                 recordAnalysis.id asc
                        """, UUID.class)
                .setParameter("userId", userId)
                .setParameter("embeddingModel", insightProperties.embeddingModel())
                .setParameter("analyzedAfter", analyzedAfter)
                .setParameter("snapshotAt", snapshotAt)
                .setMaxResults(1)
                .getResultList();

        return !result.isEmpty();
    }

    public long countEligibleRecordsAnalyzedAfter(
            UUID userId,
            LocalDateTime snapshotAt
    ) {
        return entityManager.createQuery("""
                        select count(recordAnalysis)
                        from RecordAnalysis recordAnalysis
                        join recordAnalysis.record record
                        where
                        """ + ELIGIBLE_CONDITION + """
                        and recordAnalysis.analyzedAt > :snapshotAt
                        """, Long.class)
                .setParameter("userId", userId)
                .setParameter("embeddingModel", insightProperties.embeddingModel())
                .setParameter("snapshotAt", snapshotAt)
                .getSingleResult();
    }

    public long countRecordsByAnalysisStatus(
            UUID userId,
            AiAnalysisStatus status
    ) {
        return entityManager.createQuery("""
                        select count(recordAnalysis)
                        from RecordAnalysis recordAnalysis
                        join recordAnalysis.record record
                        where record.user.id = :userId
                          and record.status =
                              com.itcotato.dortfolio.domain.record.entity.RecordStatus.COMPLETED
                          and recordAnalysis.aiAnalysisStatus = :status
                          and record.deletedAt is null
                          and record.activity.deletedAt is null
                        """, Long.class)
                .setParameter("userId", userId)
                .setParameter("status", status)
                .getSingleResult();
    }

    public List<RecordAnalysis> findAllEligible(
            UUID userId,
            LocalDateTime snapshotAt
    ) {
        return entityManager.createQuery("""
                        select recordAnalysis
                        from RecordAnalysis recordAnalysis
                        join fetch recordAnalysis.record record
                        join fetch record.template
                        where
                        """ + ELIGIBLE_CONDITION + """
                        and record.completedAt <= :snapshotAt
                        and record.updatedAt <= :snapshotAt
                        and recordAnalysis.analyzedAt <= :snapshotAt
                        order by record.completedAt asc,
                                 record.id asc
                        """, RecordAnalysis.class)
                .setParameter("userId", userId)
                .setParameter("embeddingModel", insightProperties.embeddingModel())
                .setParameter("snapshotAt", snapshotAt)
                .getResultList();
    }

    public List<RecordCompetencyTag> findStrengthTags(
            List<UUID> recordIds
    ) {
        if (recordIds.isEmpty()) {
            return List.of();
        }

        return entityManager.createQuery("""
                        select recordCompetencyTag
                        from RecordCompetencyTag recordCompetencyTag
                        join fetch recordCompetencyTag.record
                        join fetch recordCompetencyTag.competencyTag
                        where recordCompetencyTag.record.id in :recordIds
                        order by recordCompetencyTag.record.id asc,
                                 recordCompetencyTag.competencyTag.id asc
                        """, RecordCompetencyTag.class)
                .setParameter("recordIds", recordIds)
                .getResultList();
    }
}
