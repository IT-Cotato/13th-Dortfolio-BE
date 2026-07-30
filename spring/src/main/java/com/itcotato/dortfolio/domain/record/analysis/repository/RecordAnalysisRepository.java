package com.itcotato.dortfolio.domain.record.analysis.repository;

import com.itcotato.dortfolio.domain.record.analysis.entity.RecordAnalysis;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordAnalysisRepository extends JpaRepository<RecordAnalysis, UUID> {

	Optional<RecordAnalysis> findByRecord_Id(UUID recordId);

	void deleteByRecord_Id(UUID recordId);
}
