package com.itcotato.dortfolio.domain.insight.statistics;

import com.itcotato.dortfolio.domain.insight.query.AnalyzedRecordSnapshot;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

@Component
public class TemplateDistributionCalculatorImpl
        implements TemplateDistributionCalculator {

    private static final int MAX_TEMPLATE_COUNT = 4;

    private static final Comparator<TemplateAccumulator>
            TEMPLATE_COMPARATOR =
            Comparator.comparingLong(
                            TemplateAccumulator::recordCount
                    )
                    .reversed()
                    .thenComparing(
                            TemplateAccumulator::templateId
                    );

    @Override
    public List<TemplateStatistic> calculate(
            List<AnalyzedRecordSnapshot> records
    ) {
        if (records.isEmpty()) {
            return List.of();
        }

        Map<UUID, TemplateAccumulator> accumulators =
                new HashMap<>();

        // 기록은 하나의 템플릿에만 속하므로 기록마다 한 번 집계
        for (AnalyzedRecordSnapshot record : records) {
            accumulators.computeIfAbsent(
                    record.templateId(),
                    ignored -> new TemplateAccumulator(
                            record.templateId(),
                            record.templateName()
                    )
            ).add(record.templateName());
        }

        // 기록 수 내림차순, 템플릿 ID 오름차순으로 상위 4개 선택
        List<TemplateAccumulator> sorted =
                accumulators.values()
                        .stream()
                        .sorted(TEMPLATE_COMPARATOR)
                        .limit(MAX_TEMPLATE_COUNT)
                        .toList();

        return IntStream.range(0, sorted.size())
                .mapToObj(index -> toStatistic(
                        sorted.get(index),
                        records.size(),
                        index + 1
                ))
                .toList();
    }

    private TemplateStatistic toStatistic(
            TemplateAccumulator accumulator,
            int totalRecordCount,
            int rank
    ) {
        return new TemplateStatistic(
                accumulator.templateId(),
                accumulator.templateName(),
                accumulator.recordCount(),
                // 템플릿 비율 계산
                (double) accumulator.recordCount()
                        / totalRecordCount,
                rank
        );
    }

    private static class TemplateAccumulator {

        private final UUID templateId;
        private String templateName;
        private long recordCount;

        private TemplateAccumulator(
                UUID templateId,
                String templateName
        ) {
            this.templateId = templateId;
            this.templateName = templateName;
        }

        private void add(String templateName) {
            this.templateName = selectName(
                    this.templateName,
                    templateName
            );
            this.recordCount++;
        }

        private UUID templateId() {
            return templateId;
        }

        private String templateName() {
            return templateName;
        }

        private long recordCount() {
            return recordCount;
        }

        // 비정상적으로 동일 ID에 이름이 다르면 사전순 이름 사용
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