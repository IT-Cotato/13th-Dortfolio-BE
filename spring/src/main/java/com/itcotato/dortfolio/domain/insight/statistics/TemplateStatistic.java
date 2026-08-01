package com.itcotato.dortfolio.domain.insight.statistics;

import java.util.UUID;

public record TemplateStatistic(
        UUID templateId,
        String templateName,
        long recordCount,
        double ratio,
        int rank
) {
}
