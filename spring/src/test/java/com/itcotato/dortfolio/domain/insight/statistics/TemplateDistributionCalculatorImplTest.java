package com.itcotato.dortfolio.domain.insight.statistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.itcotato.dortfolio.domain.insight.query.AnalyzedRecordSnapshot;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TemplateDistributionCalculatorImplTest {

    private final TemplateDistributionCalculator calculator =
            new TemplateDistributionCalculatorImpl();

    @Test
    void returnsEmptyForEmptyRecords() {
        assertThat(calculator.calculate(List.of()))
                .isEmpty();
    }

    @Test
    void calculatesCountRatioAndRank() {
        UUID firstTemplateId = uuid(1);
        UUID secondTemplateId = uuid(2);

        List<AnalyzedRecordSnapshot> records = List.of(
                record(
                        1,
                        firstTemplateId,
                        "문제 해결 경험"
                ),
                record(
                        2,
                        firstTemplateId,
                        "문제 해결 경험"
                ),
                record(
                        3,
                        secondTemplateId,
                        "협업 경험"
                )
        );

        List<TemplateStatistic> result =
                calculator.calculate(records);

        assertThat(result).hasSize(2);

        TemplateStatistic first = result.get(0);

        assertThat(first.templateId())
                .isEqualTo(firstTemplateId);
        assertThat(first.recordCount())
                .isEqualTo(2);
        assertThat(first.ratio())
                .isCloseTo(2.0 / 3.0, within(0.000001));
        assertThat(first.rank())
                .isEqualTo(1);

        TemplateStatistic second = result.get(1);

        assertThat(second.templateId())
                .isEqualTo(secondTemplateId);
        assertThat(second.recordCount())
                .isEqualTo(1);
        assertThat(second.ratio())
                .isCloseTo(1.0 / 3.0, within(0.000001));
        assertThat(second.rank())
                .isEqualTo(2);
    }

    @Test
    void sortsTiedTemplatesByTemplateId() {
        UUID firstId = uuid(1);
        UUID secondId = uuid(2);

        List<TemplateStatistic> result =
                calculator.calculate(List.of(
                        record(1, secondId, "두 번째"),
                        record(2, firstId, "첫 번째")
                ));

        assertThat(result)
                .extracting(TemplateStatistic::templateId)
                .containsExactly(firstId, secondId);
    }

    @Test
    void returnsOnlyTopFourTemplates() {
        List<AnalyzedRecordSnapshot> records =
                new ArrayList<>();

        for (int index = 1; index <= 5; index++) {
            records.add(record(
                    index,
                    uuid(index),
                    "템플릿 " + index
            ));
        }

        List<TemplateStatistic> result =
                calculator.calculate(records);

        assertThat(result).hasSize(4);
        assertThat(result)
                .extracting(TemplateStatistic::templateId)
                .containsExactly(
                        uuid(1),
                        uuid(2),
                        uuid(3),
                        uuid(4)
                );
        assertThat(result)
                .extracting(TemplateStatistic::rank)
                .containsExactly(1, 2, 3, 4);
    }

    @Test
    void templateRatiosSumToOne() {
        List<AnalyzedRecordSnapshot> records = List.of(
                record(1, uuid(1), "첫 번째"),
                record(2, uuid(1), "첫 번째"),
                record(3, uuid(2), "두 번째"),
                record(4, uuid(3), "세 번째")
        );

        double ratioSum = calculator.calculate(records)
                .stream()
                .mapToDouble(TemplateStatistic::ratio)
                .sum();

        assertThat(ratioSum)
                .isCloseTo(1.0, within(0.000001));
    }

    @Test
    void resultDoesNotDependOnInputOrder() {
        List<AnalyzedRecordSnapshot> records =
                new ArrayList<>(List.of(
                        record(1, uuid(1), "첫 번째"),
                        record(2, uuid(2), "두 번째"),
                        record(3, uuid(1), "첫 번째")
                ));

        List<TemplateStatistic> original =
                calculator.calculate(records);

        Collections.reverse(records);

        List<TemplateStatistic> reversed =
                calculator.calculate(records);

        assertThat(reversed)
                .containsExactlyElementsOf(original);
    }

    private AnalyzedRecordSnapshot record(
            int recordNumber,
            UUID templateId,
            String templateName
    ) {
        return new AnalyzedRecordSnapshot(
                uuid(1_000 + recordNumber),
                "기록 " + recordNumber,
                LocalDateTime.of(2026, 8, 1, 10, 0)
                        .plusDays(recordNumber),
                templateId,
                templateName,
                "요약",
                List.of("근거"),
                List.of()
        );
    }

    private static UUID uuid(long value) {
        return new UUID(0L, value);
    }
}