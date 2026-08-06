package com.itcotato.dortfolio.domain.insight.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.itcotato.dortfolio.domain.activity.entity.Activity;
import com.itcotato.dortfolio.domain.activity.entity.ActivityType;
import com.itcotato.dortfolio.domain.activity.repository.ActivityRepository;
import com.itcotato.dortfolio.domain.activity.repository.ActivityTypeRepository;
import com.itcotato.dortfolio.domain.record.analysis.entity.RecordAnalysis;
import com.itcotato.dortfolio.domain.record.analysis.repository.RecordAnalysisRepository;
import com.itcotato.dortfolio.domain.record.entity.CompetencyTag;
import com.itcotato.dortfolio.domain.record.entity.Record;
import com.itcotato.dortfolio.domain.record.entity.RecordCompetencyTag;
import com.itcotato.dortfolio.domain.record.repository.CompetencyTagRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordCompetencyTagRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordRepository;
import com.itcotato.dortfolio.domain.template.entity.Template;
import com.itcotato.dortfolio.domain.template.repository.TemplateRepository;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class InsightRecordQueryRepositoryTest {

    @Autowired
    private InsightRecordQueryRepository queryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityTypeRepository activityTypeRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private RecordAnalysisRepository recordAnalysisRepository;

    @Autowired
    private CompetencyTagRepository competencyTagRepository;

    @Autowired
    private RecordCompetencyTagRepository recordCompetencyTagRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findsOnlyEligibleRecordsOwnedByUserAtSnapshot() {
        User user = createUser();
        LocalDateTime snapshotAt = LocalDateTime.of(2026, 8, 4, 12, 0);
        Record eligible = createRecord(user, "정상 기록", true);
        ReflectionTestUtils.setField(eligible, "completedAt", snapshotAt);
        recordRepository.saveAndFlush(eligible);
        completeAnalysis(eligible, true, true);

        Record future = createRecord(user, "미래 기록", true);
        ReflectionTestUtils.setField(
                future,
                "completedAt",
                snapshotAt.plusMinutes(1)
        );
        recordRepository.saveAndFlush(future);
        completeAnalysis(future, true, true);

        User anotherUser = createUser();
        createAnalyzedRecord(anotherUser, "다른 사용자 기록", true, true);
        entityManager.flush();
        entityManager.clear();

        List<RecordAnalysis> result =
                queryRepository.findAllEligible(user.getId(), snapshotAt);

        assertThat(result)
                .extracting(analysis -> analysis.getRecord().getId())
                .containsExactly(eligible.getId());
    }

    @Test
    void excludesDeletedAndIncompleteRecords() {
        User user = createUser();
        Record eligible = createAnalyzedRecord(user, "정상 기록", true, true);
        LocalDateTime snapshotAt = LocalDateTime.now().plusMinutes(1);

        Record deletedRecord = createAnalyzedRecord(user, "삭제 기록", true, true);
        deletedRecord.markDeleted(30);

        Record deletedActivityRecord = createAnalyzedRecord(
                user,
                "삭제 활동 기록",
                true,
                true
        );
        deletedActivityRecord.getActivity().markDeleted(30);

        Record deletedTemplateRecord = createAnalyzedRecord(
                user,
                "삭제 템플릿 기록",
                true,
                true
        );
        deletedTemplateRecord.getTemplate().delete();

        Record draft = createRecord(user, "임시 저장 기록", false);
        completeAnalysis(draft, true, true);

        Record pendingAnalysis = createRecord(user, "분석 중 기록", true);
        recordAnalysisRepository.save(RecordAnalysis.pending(pendingAnalysis));
        saveEmbedding(pendingAnalysis);

        Record failedAnalysis = createRecord(user, "분석 실패 기록", true);
        RecordAnalysis failed = RecordAnalysis.pending(failedAnalysis);
        failed.fail("분석 실패", false);
        recordAnalysisRepository.save(failed);
        saveEmbedding(failedAnalysis);

        entityManager.flush();
        entityManager.clear();

        List<RecordAnalysis> result =
                queryRepository.findAllEligible(user.getId(), snapshotAt);

        assertThat(result)
                .extracting(analysis -> analysis.getRecord().getId())
                .containsExactly(eligible.getId());
    }

    @Test
    void excludesRecordWithoutEmbeddingOrFreshAnalysis() {
        User user = createUser();
        Record eligible = createAnalyzedRecord(user, "정상 기록", true, true);
        createAnalyzedRecord(user, "임베딩 없는 기록", false, true);
        createAnalyzedRecord(user, "오래된 분석 기록", true, false);
        entityManager.flush();
        entityManager.clear();

        long count = queryRepository.countEligibleRecords(user.getId());
        List<RecordAnalysis> result = queryRepository.findAllEligible(
                user.getId(),
                LocalDateTime.now().plusMinutes(1)
        );

        assertThat(count).isEqualTo(1);
        assertThat(result)
                .extracting(analysis -> analysis.getRecord().getId())
                .containsExactly(eligible.getId());
    }

    @Test
    void detectsEligibleRecordAnalyzedAfterSnapshot() {
        User user = createUser();
        LocalDateTime beforeAnalysis = LocalDateTime.now().minusSeconds(1);
        createAnalyzedRecord(user, "신규 분석 기록", true, true);
        entityManager.flush();
        entityManager.clear();

        assertThat(queryRepository.existsEligibleRecordAnalyzedAfter(
                user.getId(),
                beforeAnalysis
        )).isTrue();
        assertThat(queryRepository.existsEligibleRecordAnalyzedAfter(
                user.getId(),
                LocalDateTime.now().plusSeconds(1)
        )).isFalse();
    }

    @Test
    void findsStrengthTagsInDeterministicOrder() {
        User user = createUser();
        Record record = createAnalyzedRecord(user, "강점 기록", true, true);
        CompetencyTag second = competencyTagRepository.save(
                CompetencyTag.create("협업", "협업 역량")
        );
        CompetencyTag first = competencyTagRepository.save(
                CompetencyTag.create("문제 해결", "문제 해결 역량")
        );
        recordCompetencyTagRepository.saveAll(List.of(
                RecordCompetencyTag.create(record, second, 0.8f),
                RecordCompetencyTag.create(record, first, 0.9f)
        ));
        entityManager.flush();
        entityManager.clear();

        List<UUID> firstResult =
                queryRepository.findStrengthTags(List.of(record.getId()))
                        .stream()
                        .map(tag -> tag.getCompetencyTag().getId())
                        .toList();
        List<UUID> secondResult =
                queryRepository.findStrengthTags(List.of(record.getId()))
                        .stream()
                        .map(tag -> tag.getCompetencyTag().getId())
                        .toList();

        assertThat(firstResult)
                .containsExactlyElementsOf(secondResult)
                .containsExactlyInAnyOrder(first.getId(), second.getId());
    }

    private Record createAnalyzedRecord(
            User user,
            String title,
            boolean withEmbedding,
            boolean freshAnalysis
    ) {
        Record record = createRecord(user, title, true);
        completeAnalysis(record, withEmbedding, freshAnalysis);
        return record;
    }

    private Record createRecord(User user, String title, boolean completed) {
        ActivityType activityType = activityTypeRepository.save(
                ActivityType.create(user, title + " 활동 유형")
        );
        Activity activity = activityRepository.save(Activity.create(
                user,
                activityType,
                title + " 활동",
                "설명",
                LocalDate.now(),
                null,
                true
        ));
        Template template = templateRepository.save(
                Template.createCustom(user, title + " 템플릿", "설명")
        );
        Record record = Record.builder()
                .user(user)
                .activity(activity)
                .template(template)
                .title(title)
                .build();
        if (completed) {
            record.complete();
        }
        return recordRepository.saveAndFlush(record);
    }

    private void completeAnalysis(
            Record record,
            boolean withEmbedding,
            boolean freshAnalysis
    ) {
        LocalDateTime analyzedRecordUpdatedAt = freshAnalysis
                ? record.getUpdatedAt()
                : record.getUpdatedAt().minusSeconds(1);
        RecordAnalysis analysis = RecordAnalysis.pending(record);
        analysis.complete(
                "분석 요약",
                "[\"근거 문장\"]",
                analyzedRecordUpdatedAt
        );
        recordAnalysisRepository.save(analysis);
        if (withEmbedding) {
            saveEmbedding(record);
        }
    }

    private void saveEmbedding(Record record) {
        jdbcTemplate.update("""
                        insert into record_embeddings (
                            id,
                            created_at,
                            updated_at,
                            record_id,
                            embedding_model,
                            embedding
                        ) values (
                            ?,
                            current_timestamp,
                            current_timestamp,
                            ?,
                            ?,
                            ARRAY[0.1]
                        )
                        """,
                UUID.randomUUID(),
                record.getId(),
                "text-embedding-3-large"
        );
    }

    private User createUser() {
        return userRepository.save(User.of(
                UUID.randomUUID() + "@test.com",
                "encoded-password",
                "인사이트 테스트"
        ));
    }
}
