package com.itcotato.dortfolio.domain.insight.statistics;

import com.itcotato.dortfolio.domain.insight.query.AnalyzedRecordSnapshot;

import java.util.List;

public interface TemplateDistributionCalculator {

    List<TemplateStatistic> calculate(
            List<AnalyzedRecordSnapshot> records
    );
}
