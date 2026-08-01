package com.itcotato.dortfolio.domain.insight.repository;

import com.itcotato.dortfolio.domain.insight.entity.InsightStrengthRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface InsightStrengthRecordRepository extends JpaRepository<InsightStrengthRecord, UUID> {

    List<InsightStrengthRecord> findAllByInsightStrength_Id(UUID insightStrengthId);

}
