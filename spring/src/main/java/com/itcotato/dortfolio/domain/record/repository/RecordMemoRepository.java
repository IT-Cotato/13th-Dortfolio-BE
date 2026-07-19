package com.itcotato.dortfolio.domain.record.repository;

import com.itcotato.dortfolio.domain.record.entity.RecordMemo;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordMemoRepository extends JpaRepository<RecordMemo, UUID> {

	@EntityGraph(attributePaths = {"memo", "memo.activity"})
	List<RecordMemo> findAllByRecord_IdOrderBySortOrderAsc(UUID recordId);

	void deleteAllByRecord_Id(UUID recordId);
}
