package com.itcotato.dortfolio.domain.insight.repository;

import com.itcotato.dortfolio.domain.insight.entity.InsightTemplateStatistic;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsightTemplateStatisticRepository
        extends JpaRepository<InsightTemplateStatistic, UUID> {

    List<InsightTemplateStatistic> findAllByInsight_IdOrderByRankAsc(
            UUID insightId
    );

    List<InsightTemplateStatistic>
    findAllByInsight_IdOrderByRankAscTemplateIdSnapshotAsc(
            UUID insightId
    );
}
