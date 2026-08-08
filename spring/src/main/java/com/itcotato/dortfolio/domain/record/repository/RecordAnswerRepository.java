package com.itcotato.dortfolio.domain.record.repository;

import com.itcotato.dortfolio.domain.record.entity.RecordAnswer;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecordAnswerRepository extends JpaRepository<RecordAnswer, UUID> {

	@EntityGraph(attributePaths = "templateQuestion")
	List<RecordAnswer> findAllByRecord_IdOrderByTemplateQuestion_SortOrderAsc(UUID recordId);

	@Query("""
			select answer
			from RecordAnswer answer
			join fetch answer.record record
			join fetch answer.templateQuestion question
			where record.id in :recordIds
			order by record.id asc, question.sortOrder asc
		""")
	List<RecordAnswer> findAllByRecordIdsOrderByRecordIdAndSortOrder(@Param("recordIds") Collection<UUID> recordIds);

	void deleteAllByRecord_Id(UUID recordId);
}
