package com.itcotato.dortfolio.domain.insight.query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AnalyzedRecordSnapshot(
        UUID recordId,
        String recordTitle,
        LocalDateTime completedAt,
        UUID templateId,
        String templateName,
        String summary,
        List<String> evidenceSnippets,
        List<StrengthTagSnapshot> strengthTags
) {

    public record StrengthTagSnapshot(
            UUID tagId,
            String tagName,
            float score
    ) {
    }
}
