package com.itcotato.dortfolio.domain.insight.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.itcotato.dortfolio.domain.insight.repository.InsightRecordQueryRepository;
import com.itcotato.dortfolio.domain.record.analysis.entity.RecordAnalysis;
import com.itcotato.dortfolio.domain.record.entity.Record;
import com.itcotato.dortfolio.domain.record.entity.RecordStrengthTag;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JpaAnalyzedRecordQuery implements AnalyzedRecordQuery {

    private static final TypeReference<List<String>> STRING_LIST_TYPE =
            new TypeReference<>() {
            };

    private final InsightRecordQueryRepository queryRepository;
    private final ObjectMapper objectMapper =
            JsonMapper.builder().findAndAddModules().build();

    @Override
    public List<AnalyzedRecordSnapshot> findAllForInsight(
            UUID userId,
            LocalDateTime snapshotAt
    ) {
        List<RecordAnalysis> analyses =
                queryRepository.findAllEligible(userId, snapshotAt);

        if (analyses.isEmpty()) {
            return List.of();
        }

        List<UUID> recordIds = analyses.stream()
                .map(RecordAnalysis::getRecord)
                .map(Record::getId)
                .toList();

        Map<UUID, List<RecordStrengthTag>> tagsByRecordId =
                queryRepository.findStrengthTags(recordIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                tag -> tag.getRecord().getId(),
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        return analyses.stream()
                .map(analysis -> toSnapshot(
                        analysis,
                        tagsByRecordId.getOrDefault(
                                analysis.getRecord().getId(),
                                List.of()
                        )
                ))
                .toList();
    }

    private AnalyzedRecordSnapshot toSnapshot(
            RecordAnalysis analysis,
            List<RecordStrengthTag> recordTags
    ) {
        Record record = analysis.getRecord();

        List<AnalyzedRecordSnapshot.StrengthTagSnapshot>
                strengthTags = recordTags.stream()
                .map(this::toStrengthTagSnapshot)
                .distinct()
                .toList();

        return new AnalyzedRecordSnapshot(
                record.getId(),
                record.getTitle(),
                record.getCompletedAt(),
                record.getTemplate().getId(),
                record.getTemplate().getTitle(),
                analysis.getSummary(),
                parseEvidenceSnippets(
                        analysis.getEvidenceSnippets()
                ),
                strengthTags
        );
    }

    private AnalyzedRecordSnapshot.StrengthTagSnapshot toStrengthTagSnapshot(
            RecordStrengthTag recordTag
    ) {
        return new AnalyzedRecordSnapshot.StrengthTagSnapshot(
                recordTag.getStrengthTag().getId(),
                recordTag.getStrengthTag().getName(),
                recordTag.getCosineSimilarity()
        );
    }

    private List<String> parseEvidenceSnippets(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        try {
            List<String> snippets = objectMapper.readValue(
                    json,
                    STRING_LIST_TYPE
            );
            return snippets != null ? snippets : List.of();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to parse record analysis evidence snippets.",
                    exception
            );
        }
    }
}
