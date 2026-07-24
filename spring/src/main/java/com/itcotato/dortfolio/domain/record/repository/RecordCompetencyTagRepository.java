package com.itcotato.dortfolio.domain.record.repository;

import com.itcotato.dortfolio.domain.record.entity.RecordCompetencyTag;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordCompetencyTagRepository extends JpaRepository<RecordCompetencyTag, UUID> {

	List<RecordCompetencyTag> findAllByRecord_Id(UUID recordId);

	void deleteAllByRecord_Id(UUID recordId);
}
