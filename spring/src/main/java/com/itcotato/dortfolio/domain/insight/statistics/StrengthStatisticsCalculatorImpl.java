package com.itcotato.dortfolio.domain.insight.statistics;

import com.itcotato.dortfolio.domain.insight.query.AnalyzedRecordSnapshot;
import com.itcotato.dortfolio.domain.insight.query.AnalyzedRecordSnapshot.StrengthTagSnapshot;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

@Component
public class StrengthStatisticsCalculatorImpl
        implements StrengthStatisticsCalculator {

    private static final int MAX_STRENGTH_COUNT = 5;

    private static final Comparator<StrengthAccumulator>
            STRENGTH_COMPARATOR =
            Comparator.comparingLong(
                            StrengthAccumulator::recordCount
                    )
                    .reversed()
                    .thenComparing(
                            Comparator.comparingDouble(
                                    StrengthAccumulator::averageScore
                            ).reversed()
                    )
                    .thenComparing(
                            StrengthAccumulator::tagId
                    );

    @Override
    public List<StrengthStatistic> calculate(
            List<AnalyzedRecordSnapshot> records
    ) {
        if (records.isEmpty()) {
            return List.of();
        }

        Map<UUID, StrengthAccumulator> accumulators =
                new HashMap<>();

        for (AnalyzedRecordSnapshot record : records) {
            Map<UUID, StrengthTagSnapshot> uniqueTags =
                    deduplicateTags(record.strengthTags());

            for (StrengthTagSnapshot tag : uniqueTags.values()) {
                accumulators.computeIfAbsent(
                        tag.tagId(),
                        ignored -> new StrengthAccumulator(
                                tag.tagId(),
                                tag.tagName()
                        )
                ).add(tag.tagName(), tag.score());
            }
        }

        // 기록 수, 평균 점수, 태그 ID 순으로 정렬해 상위 5개를 선택
        List<StrengthAccumulator> sorted = accumulators.values()
                .stream()
                .sorted(STRENGTH_COMPARATOR)
                .limit(MAX_STRENGTH_COUNT)
                .toList();

        return IntStream.range(0, sorted.size())
                .mapToObj(index -> toStatistic(
                        sorted.get(index),
                        records.size(),
                        index + 1
                ))
                .toList();
    }

    private Map<UUID, StrengthTagSnapshot> deduplicateTags(
            List<StrengthTagSnapshot> tags
    ) {
        Map<UUID, StrengthTagSnapshot> uniqueTags =
                new HashMap<>();

        for (StrengthTagSnapshot tag : tags) {
            uniqueTags.merge(
                    tag.tagId(),
                    tag,
                    this::selectRepresentative
            );
        }

        return uniqueTags;
    }

    /*
     * 같은 기록에 동일 태그가 중복되면 가장 높은 점수를 사용
     * 점수까지 같으면 이름순으로 선택해 입력 순서에 영향을 받지 않게
     */
    private StrengthTagSnapshot selectRepresentative(
            StrengthTagSnapshot first,
            StrengthTagSnapshot second
    ) {
        int scoreComparison = Float.compare(
                first.score(),
                second.score()
        );

        if (scoreComparison > 0) {
            return first;
        }

        if (scoreComparison < 0) {
            return second;
        }

        return first.tagName().compareTo(second.tagName()) <= 0
                ? first
                : second;
    }

    private StrengthStatistic toStatistic(
            StrengthAccumulator accumulator,
            int totalRecordCount,
            int rank
    ) {
        return new StrengthStatistic(
                accumulator.tagId(),
                accumulator.tagName(),
                accumulator.recordCount(),
                accumulator.averageScore(),
                // 강점 비율 계산
                (double) accumulator.recordCount()
                        / totalRecordCount,
                rank
        );
    }

    private static class StrengthAccumulator {

        private final UUID tagId;
        private String tagName;
        private long recordCount;
        private double scoreSum;

        private StrengthAccumulator(
                UUID tagId,
                String tagName
        ) {
            this.tagId = tagId;
            this.tagName = tagName;
        }

        private void add(String tagName, float score) {
            this.tagName = selectName(
                    this.tagName,
                    tagName
            );
            this.recordCount++;
            this.scoreSum += score;
        }

        private UUID tagId() {
            return tagId;
        }

        private String tagName() {
            return tagName;
        }

        private long recordCount() {
            return recordCount;
        }

        private double averageScore() {
            return scoreSum / recordCount;
        }

        private static String selectName(
                String first,
                String second
        ) {
            return first.compareTo(second) <= 0
                    ? first
                    : second;
        }
    }
}