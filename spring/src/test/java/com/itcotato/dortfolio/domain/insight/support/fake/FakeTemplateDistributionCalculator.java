package com.itcotato.dortfolio.domain.insight.support.fake;

import com.itcotato.dortfolio.domain.insight.query.AnalyzedRecordSnapshot;
import com.itcotato.dortfolio.domain.insight.statistics.TemplateDistributionCalculator;
import com.itcotato.dortfolio.domain.insight.statistics.TemplateStatistic;

import java.util.List;

public class FakeTemplateDistributionCalculator implements TemplateDistributionCalculator {

    private List<TemplateStatistic> result = List.of();
    private List<AnalyzedRecordSnapshot> requestedRecords = List.of();

    public void setResult(List<TemplateStatistic> result) {
        this.result = List.copyOf(result);
    }

    public List<AnalyzedRecordSnapshot> getRequestedRecords() {
        return requestedRecords;
    }

    @Override
    public List<TemplateStatistic> calculate(
            List<AnalyzedRecordSnapshot> records
    ) {
        this.requestedRecords = List.copyOf(records);
        return result;
    }
}
