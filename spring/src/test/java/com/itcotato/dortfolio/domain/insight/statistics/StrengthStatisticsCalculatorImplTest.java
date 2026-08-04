package com.itcotato.dortfolio.domain.insight.statistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.itcotato.dortfolio.domain.insight.query.AnalyzedRecordSnapshot;
import com.itcotato.dortfolio.domain.insight.query.AnalyzedRecordSnapshot.StrengthTagSnapshot;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StrengthStatisticsCalculatorImplTest {

    private static final UUID TEMPLATE_ID =
            uuid(100);

    private final StrengthStatisticsCalculator calculator =
            new StrengthStatisticsCalculatorImpl();

    @Test
    void returnsEmptyForEmptyRecords() {
        assertThat(calculator.calculate(List.of()))
                .isEmpty();
    }

    @Test
    void calculatesCountAverageRatioAndRank() {
        UUID problemSolvingId = uuid(1);
        UUID collaborationId = uuid(2);

        List<AnalyzedRecordSnapshot> records = List.of(
                record(
                        1,
                        List.of(
                                tag(
                                        problemSolvingId,
                                        "문제 해결",
                                        0.9f
                                ),
                                tag(
                                        collaborationId,
                                        "협업",
                                        0.8f
                                )
                        )
                ),
                record(
                        2,
                        List.of(
                                tag(
                                        problemSolvingId,
                                        "문제 해결",
                                        0.5f
                                )
                        )
                ),
                record(3, List.of())
        );

        List<StrengthStatistic> result =
                calculator.calculate(records);

        assertThat(result).hasSize(2);

        StrengthStatistic first = result.get(0);

        assertThat(first.tagId())
                .isEqualTo(problemSolvingId);
        assertThat(first.tagName())
                .isEqualTo("문제 해결");
        assertThat(first.recordCount())
                .isEqualTo(2);
        assertThat(first.averageScore())
                .isCloseTo(0.7, within(0.000001));
        assertThat(first.ratio())
                .isCloseTo(2.0 / 3.0, within(0.000001));
        assertThat(first.rank())
                .isEqualTo(1);

        StrengthStatistic second = result.get(1);

        assertThat(second.tagId())
                .isEqualTo(collaborationId);
        assertThat(second.recordCount())
                .isEqualTo(1);
        assertThat(second.averageScore())
                .isCloseTo(0.8, within(0.000001));
        assertThat(second.ratio())
                .isCloseTo(1.0 / 3.0, within(0.000001));
        assertThat(second.rank())
                .isEqualTo(2);
    }

    @Test
    void countsDuplicatedTagOnlyOncePerRecord() {
        UUID tagId = uuid(1);

        AnalyzedRecordSnapshot record = record(
                1,
                List.of(
                        tag(tagId, "문제 해결", 0.7f),
                        tag(tagId, "문제 해결", 0.9f)
                )
        );

        StrengthStatistic result =
                calculator.calculate(List.of(record)).get(0);

        assertThat(result.recordCount()).isEqualTo(1);
        assertThat(result.averageScore())
                .isCloseTo(0.9, within(0.000001));
        assertThat(result.ratio())
                .isEqualTo(1.0);
    }

    @Test
    void sortsByCountAverageScoreAndTagId() {
        UUID firstId = uuid(1);
        UUID secondId = uuid(2);
        UUID thirdId = uuid(3);

        List<AnalyzedRecordSnapshot> records = List.of(
                record(
                        1,
                        List.of(
                                tag(firstId, "첫 번째", 0.7f),
                                tag(secondId, "두 번째", 0.9f),
                                tag(thirdId, "세 번째", 0.9f)
                        )
                ),
                record(
                        2,
                        List.of(
                                tag(firstId, "첫 번째", 0.7f)
                        )
                )
        );

        List<StrengthStatistic> result =
                calculator.calculate(records);

        assertThat(result)
                .extracting(StrengthStatistic::tagId)
                .containsExactly(
                        firstId,   // recordCount 2
                        secondId,  // count 1, average 0.9, ID가 앞
                        thirdId
                );
    }

    @Test
    void returnsOnlyTopFiveStrengths() {
        List<StrengthTagSnapshot> tags =
                new ArrayList<>();

        for (int index = 1; index <= 6; index++) {
            tags.add(tag(
                    uuid(index),
                    "강점 " + index,
                    0.5f
            ));
        }

        List<StrengthStatistic> result =
                calculator.calculate(
                        List.of(record(1, tags))
                );

        assertThat(result).hasSize(5);
        assertThat(result)
                .extracting(StrengthStatistic::tagId)
                .containsExactly(
                        uuid(1),
                        uuid(2),
                        uuid(3),
                        uuid(4),
                        uuid(5)
                );
        assertThat(result)
                .extracting(StrengthStatistic::rank)
                .containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    void resultDoesNotDependOnInputOrder() {
        List<AnalyzedRecordSnapshot> records =
                new ArrayList<>(List.of(
                        record(
                                1,
                                List.of(
                                        tag(uuid(1), "문제 해결", 0.8f)
                                )
                        ),
                        record(
                                2,
                                List.of(
                                        tag(uuid(2), "협업", 0.9f)
                                )
                        )
                ));

        List<StrengthStatistic> original =
                calculator.calculate(records);

        Collections.reverse(records);

        List<StrengthStatistic> reversed =
                calculator.calculate(records);

        assertThat(reversed)
                .containsExactlyElementsOf(original);
    }

    private AnalyzedRecordSnapshot record(
            int recordNumber,
            List<StrengthTagSnapshot> tags
    ) {
        return new AnalyzedRecordSnapshot(
                uuid(1_000 + recordNumber),
                "기록 " + recordNumber,
                LocalDateTime.of(2026, 8, 1, 10, 0)
                        .plusDays(recordNumber),
                TEMPLATE_ID,
                "문제 해결 경험",
                "요약",
                List.of("근거"),
                tags
        );
    }

    private StrengthTagSnapshot tag(
            UUID id,
            String name,
            float score
    ) {
        return new StrengthTagSnapshot(
                id,
                name,
                score
        );
    }

    private static UUID uuid(long value) {
        return new UUID(0L, value);
    }
}