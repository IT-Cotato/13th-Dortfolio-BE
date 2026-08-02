package com.itcotato.dortfolio.domain.insight.statistics;

import java.util.UUID;

public record StrengthStatistic(
        UUID tagId,
        String tagName,
        long recordCount,
        double averageScore,
        double ratio,
        int rank
) {
}
