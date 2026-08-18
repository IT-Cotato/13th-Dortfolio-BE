package com.itcotato.dortfolio.domain.insight.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.itcotato.dortfolio.domain.insight.repository.InsightRecordQueryRepository;
import com.itcotato.dortfolio.domain.record.analysis.entity.RecordAnalysis;
import com.itcotato.dortfolio.domain.record.entity.StrengthTag;
import com.itcotato.dortfolio.domain.record.entity.Record;
import com.itcotato.dortfolio.domain.record.entity.RecordStrengthTag;
import com.itcotato.dortfolio.domain.template.entity.Template;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import static org.mockito.ArgumentMatchers.anyList;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JpaAnalyzedRecordQueryTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID RECORD_ID = UUID.randomUUID();
    private static final UUID TEMPLATE_ID = UUID.randomUUID();
    private static final UUID TAG_ID = UUID.randomUUID();
    private static final LocalDateTime SNAPSHOT_AT =
            LocalDateTime.of(2026, 8, 4, 12, 0);

    @Mock
    private InsightRecordQueryRepository queryRepository;

    @Mock
    private RecordAnalysis analysis;

    @Mock
    private Record record;

    @Mock
    private Template template;

    @Mock
    private RecordStrengthTag recordTag;

    @Mock
    private StrengthTag strengthTag;

    private JpaAnalyzedRecordQuery analyzedRecordQuery;

    @BeforeEach
    void setUp() {
        analyzedRecordQuery = new JpaAnalyzedRecordQuery(queryRepository);
    }

    @Test
    void returnsEmptyWithoutLoadingStrengthTagsWhenEligibleRecordDoesNotExist() {
        given(queryRepository.findAllEligible(USER_ID, SNAPSHOT_AT))
                .willReturn(List.of());

        List<AnalyzedRecordSnapshot> result =
                analyzedRecordQuery.findAllForInsight(USER_ID, SNAPSHOT_AT);

        assertThat(result).isEmpty();

        then(queryRepository).should(never()).findStrengthTags(anyList());
    }

    @Test
    void convertsAnalysisAndStrengthTagToSnapshot() {
        givenAnalysis("[\"병목을 분석했습니다.\",\"캐시를 적용했습니다.\"]");
        given(queryRepository.findAllEligible(USER_ID, SNAPSHOT_AT))
                .willReturn(List.of(analysis));
        given(queryRepository.findStrengthTags(List.of(RECORD_ID)))
                .willReturn(List.of(recordTag));

        List<AnalyzedRecordSnapshot> result =
                analyzedRecordQuery.findAllForInsight(USER_ID, SNAPSHOT_AT);

        assertThat(result).singleElement().satisfies(snapshot -> {
            assertThat(snapshot.recordId()).isEqualTo(RECORD_ID);
            assertThat(snapshot.recordTitle()).isEqualTo("캐시 성능 개선");
            assertThat(snapshot.templateId()).isEqualTo(TEMPLATE_ID);
            assertThat(snapshot.templateName()).isEqualTo("문제 해결 경험");
            assertThat(snapshot.summary()).isEqualTo("응답 시간을 개선했습니다.");
            assertThat(snapshot.evidenceSnippets()).containsExactly(
                    "병목을 분석했습니다.",
                    "캐시를 적용했습니다."
            );
            assertThat(snapshot.strengthTags()).containsExactly(
                    new AnalyzedRecordSnapshot.StrengthTagSnapshot(
                            TAG_ID,
                            "문제 해결",
                            0.9f
                    )
            );
        });
    }

    @Test
    void removesDuplicatedStrengthTagSnapshots() {
        givenAnalysis("[\"근거\"]");
        given(queryRepository.findAllEligible(USER_ID, SNAPSHOT_AT))
                .willReturn(List.of(analysis));
        given(queryRepository.findStrengthTags(List.of(RECORD_ID)))
                .willReturn(List.of(recordTag, recordTag));

        AnalyzedRecordSnapshot result = analyzedRecordQuery
                .findAllForInsight(USER_ID, SNAPSHOT_AT)
                .get(0);

        assertThat(result.strengthTags()).hasSize(1);
    }

    @Test
    void throwsWhenEvidenceSnippetsJsonIsInvalid() {
        givenAnalysis("invalid-json");
        given(queryRepository.findAllEligible(USER_ID, SNAPSHOT_AT))
                .willReturn(List.of(analysis));
        given(queryRepository.findStrengthTags(List.of(RECORD_ID)))
                .willReturn(List.of());

        assertThatThrownBy(() ->
                analyzedRecordQuery.findAllForInsight(USER_ID, SNAPSHOT_AT)
        ).isInstanceOf(IllegalStateException.class);
    }

    private void givenAnalysis(String evidenceSnippets) {
        given(analysis.getRecord()).willReturn(record);
        given(analysis.getSummary()).willReturn("응답 시간을 개선했습니다.");
        given(analysis.getEvidenceSnippets()).willReturn(evidenceSnippets);
        given(record.getId()).willReturn(RECORD_ID);
        given(record.getTitle()).willReturn("캐시 성능 개선");
        given(record.getCompletedAt()).willReturn(SNAPSHOT_AT.minusDays(1));
        given(record.getTemplate()).willReturn(template);
        given(template.getId()).willReturn(TEMPLATE_ID);
        given(template.getTitle()).willReturn("문제 해결 경험");
        given(recordTag.getRecord()).willReturn(record);
        given(recordTag.getStrengthTag()).willReturn(strengthTag);
        given(recordTag.getCosineSimilarity()).willReturn(0.9f);
        given(strengthTag.getId()).willReturn(TAG_ID);
        given(strengthTag.getName()).willReturn("문제 해결");
    }
}
