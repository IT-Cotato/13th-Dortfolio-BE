package com.itcotato.dortfolio.domain.insight.repository;

import com.itcotato.dortfolio.domain.insight.entity.InsightTemplateStatistic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InsightTemplateStatisticRepository extends JpaRepository<InsightTemplateStatistic, UUID> {

    List<InsightTemplateStatistic> findAllByInsight_IdOrderByRankAsc(UUID insightId);

}
