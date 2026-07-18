package com.itcotato.dortfolio.domain.record.repository;

import com.itcotato.dortfolio.domain.record.entity.RecordAnswer;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordAnswerRepository extends JpaRepository<RecordAnswer, UUID> {

	List<RecordAnswer> findAllByRecord_IdOrderBySortOrderAsc(UUID recordId);

	void deleteAllByRecord_Id(UUID recordId);
}
