package com.itcotato.dortfolio.domain.record.repository;

import com.itcotato.dortfolio.domain.record.entity.RecordStrengthTag;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecordStrengthTagRepository extends JpaRepository<RecordStrengthTag, UUID> {

	List<RecordStrengthTag> findAllByRecord_Id(UUID recordId);

	@Modifying
	@Query("delete from RecordStrengthTag recordTag where recordTag.record.id = :recordId")
	void deleteAllByRecord_Id(@Param("recordId") UUID recordId);
}
