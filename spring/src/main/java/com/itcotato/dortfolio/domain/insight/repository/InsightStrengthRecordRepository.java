package com.itcotato.dortfolio.domain.insight.repository;

import com.itcotato.dortfolio.domain.insight.entity.InsightStrengthRecord;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InsightStrengthRecordRepository
        extends JpaRepository<InsightStrengthRecord, UUID> {

    List<InsightStrengthRecord>
    findAllByInsightStrength_Id(
            UUID insightStrengthId
    );

    /* 한 Insight의 모든 강점 연결 기록을 한 번에 조회 */
    @Query("""
            select strengthRecord
            from InsightStrengthRecord strengthRecord
            join fetch strengthRecord.insightStrength strength
            where strength.insight.id = :insightId
            order by
                strength.rank asc,
                strengthRecord.recordCompletedAtSnapshot desc,
                strengthRecord.recordIdSnapshot asc
            """)
    List<InsightStrengthRecord> findAllForInsight(
            @Param("insightId") UUID insightId
    );
}