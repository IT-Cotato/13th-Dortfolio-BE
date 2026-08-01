package com.itcotato.dortfolio.domain.insight.recommendation;

import java.util.List;
import java.util.UUID;

public record RecommendationCandidate(
        UUID recordId,
        String recordTitle,
        String templateName,
        String summary,
        List<String> evidenceSnippets,
        double similarity
) {
}
