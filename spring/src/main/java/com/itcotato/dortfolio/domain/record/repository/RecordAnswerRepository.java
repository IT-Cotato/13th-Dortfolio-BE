package com.itcotato.dortfolio.domain.record.repository;

import com.itcotato.dortfolio.domain.record.entity.RecordAnswer;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecordAnswerRepository extends JpaRepository<RecordAnswer, UUID> {

	List<RecordAnswer> findAllByRecord_IdOrderBySortOrderAsc(UUID recordId);

	@Query("""
			select answer
			from RecordAnswer answer
			join fetch answer.record record
			where record.id in :recordIds
			order by record.id asc, answer.sortOrder asc
		""")
	List<RecordAnswer> findAllByRecordIdsOrderByRecordIdAndSortOrder(@Param("recordIds") Collection<UUID> recordIds);

	void deleteAllByRecord_Id(UUID recordId);
}
