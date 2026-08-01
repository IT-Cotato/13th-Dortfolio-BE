package com.itcotato.dortfolio.domain.insight.repository;

import com.itcotato.dortfolio.domain.insight.entity.InsightStrength;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InsightStrengthRepository extends JpaRepository<InsightStrength, UUID> {

    List<InsightStrength> findAllByInsight_IdOrderByRankAsc(UUID insightId);
}
