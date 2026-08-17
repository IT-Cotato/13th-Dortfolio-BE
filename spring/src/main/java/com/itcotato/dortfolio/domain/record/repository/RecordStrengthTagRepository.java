package com.itcotato.dortfolio.domain.record.repository;

import com.itcotato.dortfolio.domain.record.entity.RecordStrengthTag;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordStrengthTagRepository extends JpaRepository<RecordStrengthTag, UUID> {

	List<RecordStrengthTag> findAllByRecord_Id(UUID recordId);

	void deleteAllByRecord_Id(UUID recordId);
}
