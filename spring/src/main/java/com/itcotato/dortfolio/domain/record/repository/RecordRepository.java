package com.itcotato.dortfolio.domain.record.repository;

import com.itcotato.dortfolio.domain.record.entity.Record;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecordRepository extends JpaRepository<Record, UUID>, RecordRepositoryCustom {

	@EntityGraph(attributePaths = {"activity", "template"})
	Optional<Record> findByIdAndUser_Id(UUID id, UUID userId);

	@EntityGraph(attributePaths = {"activity", "template"})
	Optional<Record> findByIdAndUser_IdAndDeletedAtIsNull(UUID id, UUID userId);

    @Query("""
        select count(record)
        from Record record
        where record.user.id = :userId
          and record.status =
              com.itcotato.dortfolio.domain.record.entity.RecordStatus.COMPLETED
          and record.deletedAt is null
          and record.activity.deletedAt is null
          and record.template.deletedAt is null
        """)
    long countAvailableCompletedByUserId(
            @Param("userId") UUID userId
    );
}
