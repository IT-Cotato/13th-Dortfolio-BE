package com.itcotato.dortfolio.domain.insight.repository;

import com.itcotato.dortfolio.domain.insight.entity.InsightJobRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InsightJobRecommendationRepository extends JpaRepository<InsightJobRecommendation, UUID> {

    List<InsightJobRecommendation> findAllByInsight_Id(UUID insightId);

}
