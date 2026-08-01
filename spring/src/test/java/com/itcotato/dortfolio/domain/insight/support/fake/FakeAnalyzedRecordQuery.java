package com.itcotato.dortfolio.domain.insight.support.fake;

import com.itcotato.dortfolio.domain.insight.query.AnalyzedRecordQuery;
import com.itcotato.dortfolio.domain.insight.query.AnalyzedRecordSnapshot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class FakeAnalyzedRecordQuery implements AnalyzedRecordQuery {

    private List<AnalyzedRecordSnapshot> records = List.of();
    private UUID requestedUserId;
    private LocalDateTime requestedSnapshotAt;

    public void setRecords(List<AnalyzedRecordSnapshot> records) {
        this.records = List.copyOf(records);
    }

    public UUID getRequestedUserId() {
        return requestedUserId;
    }

    public LocalDateTime getRequestedSnapshotAt() {
        return requestedSnapshotAt;
    }

    @Override
    public List<AnalyzedRecordSnapshot> findAllForInsight(
            UUID userId,
            LocalDateTime snapshotAt
    ) {
        this.requestedUserId = userId;
        this.requestedSnapshotAt = snapshotAt;

        return records;
    }
}
