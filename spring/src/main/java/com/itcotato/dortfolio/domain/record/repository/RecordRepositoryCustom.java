package com.itcotato.dortfolio.domain.record.repository;

import com.itcotato.dortfolio.domain.record.dto.req.RecordSearchCondition;
import com.itcotato.dortfolio.domain.record.entity.Record;
import java.util.List;
import java.util.UUID;

public interface RecordRepositoryCustom {

	List<Record> searchRecords(UUID userId, RecordSearchCondition condition);

	List<Record> searchRecords(UUID userId, RecordSearchCondition condition, int page, int size);

	long countRecords(UUID userId, RecordSearchCondition condition);

	List<Record> findRecentRecords(UUID userId, int limit);
}
