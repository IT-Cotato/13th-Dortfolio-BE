package com.itcotato.dortfolio.domain.record.repository;

import com.itcotato.dortfolio.domain.record.entity.RecordEmbedding;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecordEmbeddingRepository extends JpaRepository<RecordEmbedding, UUID> {

	long countByRecord_Id(UUID recordId);

	@Modifying
	@Query("delete from RecordEmbedding embedding where embedding.record.id = :recordId")
	void deleteAllByRecord_Id(@Param("recordId") UUID recordId);
}
