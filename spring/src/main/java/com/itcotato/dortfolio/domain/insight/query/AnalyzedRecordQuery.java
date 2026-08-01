package com.itcotato.dortfolio.domain.insight.query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AnalyzedRecordQuery {

    List<AnalyzedRecordSnapshot> findAllForInsight(
            UUID userId,
            LocalDateTime snapshotAt
    );
}
