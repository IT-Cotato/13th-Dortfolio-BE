package com.itcotato.dortfolio.domain.record.repository;

import com.itcotato.dortfolio.domain.record.entity.Record;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordRepository extends JpaRepository<Record, UUID>, RecordRepositoryCustom {

	@EntityGraph(attributePaths = {"activity", "template"})
	Optional<Record> findByIdAndUser_Id(UUID id, UUID userId);

	@EntityGraph(attributePaths = {"activity", "template"})
	Optional<Record> findByIdAndUser_IdAndDeletedAtIsNull(UUID id, UUID userId);
}
