package com.itcotato.dortfolio.domain.insight.repository;

import com.itcotato.dortfolio.domain.insight.entity.InsightJobRecommendation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsightJobRecommendationRepository
        extends JpaRepository<InsightJobRecommendation, UUID> {

    List<InsightJobRecommendation> findAllByInsight_Id(
            UUID insightId
    );

    List<InsightJobRecommendation>
    findAllByInsight_IdOrderBySortOrderSnapshotAscJobCompetencyIdSnapshotAsc(
            UUID insightId
    );
}
