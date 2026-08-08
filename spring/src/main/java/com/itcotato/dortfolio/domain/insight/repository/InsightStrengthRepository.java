package com.itcotato.dortfolio.domain.insight.repository;

import com.itcotato.dortfolio.domain.insight.entity.InsightStrength;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsightStrengthRepository
        extends JpaRepository<InsightStrength, UUID> {

    List<InsightStrength> findAllByInsight_IdOrderByRankAsc(
            UUID insightId
    );

    List<InsightStrength>
    findAllByInsight_IdOrderByRankAscStrengthTagIdSnapshotAsc(
            UUID insightId
    );
}
