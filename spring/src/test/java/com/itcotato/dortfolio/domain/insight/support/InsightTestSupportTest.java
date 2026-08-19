package com.itcotato.dortfolio.domain.insight.support;

import static com.itcotato.dortfolio.domain.insight.support.fixture.InsightTestFixture.SNAPSHOT_AT;
import static com.itcotato.dortfolio.domain.insight.support.fixture.InsightTestFixture.USER_ID;
import static com.itcotato.dortfolio.domain.insight.support.fixture.InsightTestFixture.analyzedRecord;
import static com.itcotato.dortfolio.domain.insight.support.fixture.InsightTestFixture.availableEligibility;
import static com.itcotato.dortfolio.domain.insight.support.fixture.InsightTestFixture.recommendationRequest;
import static com.itcotato.dortfolio.domain.insight.support.fixture.InsightTestFixture.recommendationResult;
import static com.itcotato.dortfolio.domain.insight.support.fixture.InsightTestFixture.strengthStatistic;
import static com.itcotato.dortfolio.domain.insight.support.fixture.InsightTestFixture.templateStatistic;
import static org.assertj.core.api.Assertions.assertThat;

import com.itcotato.dortfolio.domain.insight.query.AnalyzedRecordSnapshot;
import com.itcotato.dortfolio.domain.insight.support.fake.FakeAnalyzedRecordQuery;
import com.itcotato.dortfolio.domain.insight.support.fake.FakeInsightEligibilityChecker;
import com.itcotato.dortfolio.domain.insight.support.fake.FakeInsightRecommendationGenerator;
import com.itcotato.dortfolio.domain.insight.support.fake.FakeStrengthStatisticsCalculator;
import com.itcotato.dortfolio.domain.insight.support.fake.FakeTemplateDistributionCalculator;
import java.util.List;
import org.junit.jupiter.api.Test;

public class InsightTestSupportTest {

    @Test
    void eligibilityFakeReturnsConfiguredResponse() {
        FakeInsightEligibilityChecker checker =
                new FakeInsightEligibilityChecker();

        checker.setResponse(availableEligibility());

        assertThat(checker.check(USER_ID))
                .isEqualTo(availableEligibility());
        assertThat(checker.getRequestedUserId())
                .isEqualTo(USER_ID);
    }

    @Test
    void analyzedRecordQueryFakeReturnsConfiguredRecords() {
        FakeAnalyzedRecordQuery query =
                new FakeAnalyzedRecordQuery();

        List<AnalyzedRecordSnapshot> records =
                List.of(analyzedRecord());

        query.setRecords(records);

        assertThat(query.findAllForInsight(USER_ID, SNAPSHOT_AT))
                .containsExactlyElementsOf(records);
        assertThat(query.getRequestedUserId())
                .isEqualTo(USER_ID);
        assertThat(query.getRequestedSnapshotAt())
                .isEqualTo(SNAPSHOT_AT);
    }

    @Test
    void statisticsFakesReturnConfiguredResults() {
        List<AnalyzedRecordSnapshot> records =
                List.of(analyzedRecord());

        FakeStrengthStatisticsCalculator strengthCalculator =
                new FakeStrengthStatisticsCalculator();
        strengthCalculator.setResult(
                List.of(strengthStatistic())
        );

        FakeTemplateDistributionCalculator templateCalculator =
                new FakeTemplateDistributionCalculator();
        templateCalculator.setResult(
                List.of(templateStatistic())
        );

        assertThat(strengthCalculator.calculate(records))
                .containsExactly(strengthStatistic());
        assertThat(strengthCalculator.getRequestedRecords())
                .containsExactlyElementsOf(records);

        assertThat(templateCalculator.calculate(records))
                .containsExactly(templateStatistic());
        assertThat(templateCalculator.getRequestedRecords())
                .containsExactlyElementsOf(records);
    }

    @Test
    void recommendationFakeReturnsConfiguredResult() {
        FakeInsightRecommendationGenerator generator =
                new FakeInsightRecommendationGenerator();

        generator.setResult(recommendationResult());

        assertThat(generator.generate(recommendationRequest()))
                .containsExactly(recommendationResult());
        assertThat(generator.getRequestedRequest())
                .isEqualTo(recommendationRequest());
    }
}
