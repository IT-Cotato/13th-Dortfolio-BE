package com.itcotato.dortfolio.domain.insight.support.fake;

import com.itcotato.dortfolio.domain.insight.query.AnalyzedRecordSnapshot;
import com.itcotato.dortfolio.domain.insight.statistics.StrengthStatistic;
import com.itcotato.dortfolio.domain.insight.statistics.StrengthStatisticsCalculator;

import java.util.List;

public class FakeStrengthStatisticsCalculator implements StrengthStatisticsCalculator {

    private List<StrengthStatistic> result = List.of();
    private List<AnalyzedRecordSnapshot> requestedRecords = List.of();

    public void setResult(List<StrengthStatistic> result) {
        this.result = List.copyOf(result);
    }

    public List<AnalyzedRecordSnapshot> getRequestedRecords() {
        return requestedRecords;
    }

    @Override
    public List<StrengthStatistic> calculate(
            List<AnalyzedRecordSnapshot> records
    ) {
        this.requestedRecords = List.copyOf(records);
        return result;
    }
}
