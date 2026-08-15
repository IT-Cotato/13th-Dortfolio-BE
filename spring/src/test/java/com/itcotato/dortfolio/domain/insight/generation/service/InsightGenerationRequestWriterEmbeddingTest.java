package com.itcotato.dortfolio.domain.insight.generation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itcotato.dortfolio.domain.insight.config.InsightProperties;
import com.itcotato.dortfolio.domain.insight.dto.res.InsightEligibilityReason;
import com.itcotato.dortfolio.domain.insight.dto.res.InsightEligibilityResponse;
import com.itcotato.dortfolio.domain.insight.entity.Insight;
import com.itcotato.dortfolio.domain.insight.generation.model.InsightGenerationCommand;
import com.itcotato.dortfolio.domain.insight.repository.InsightRepository;
import com.itcotato.dortfolio.domain.insight.service.InsightEligibilityChecker;
import com.itcotato.dortfolio.domain.job.entity.Job;
import com.itcotato.dortfolio.domain.job.entity.JobCompetency;
import com.itcotato.dortfolio.domain.job.repository.JobCompetencyEmbeddingRepository;
import com.itcotato.dortfolio.domain.job.repository.JobCompetencyRepository;
import com.itcotato.dortfolio.domain.record.entity.CompetencyTag;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.entity.UserJob;
import com.itcotato.dortfolio.domain.user.repository.UserJobRepository;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.JobErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InsightGenerationRequestWriterEmbeddingTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID JOB_ID = UUID.randomUUID();
    private static final LocalDateTime SNAPSHOT_AT =
            LocalDateTime.of(2026, 8, 15, 12, 0);

    @Mock
    private InsightEligibilityChecker eligibilityChecker;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserJobRepository userJobRepository;
    @Mock
    private JobCompetencyRepository jobCompetencyRepository;
    @Mock
    private JobCompetencyEmbeddingRepository embeddingRepository;
    @Mock
    private InsightRepository insightRepository;
    @Mock
    private InsightProperties insightProperties;

    private InsightGenerationRequestWriter writer;
    private List<JobCompetency> competencies;

    @BeforeEach
    void setUp() {
        writer = new InsightGenerationRequestWriter(
                eligibilityChecker,
                userRepository,
                userJobRepository,
                jobCompetencyRepository,
                embeddingRepository,
                insightRepository,
                insightProperties
        );
        competencies = IntStream.rangeClosed(1, 5)
                .mapToObj(this::competency)
                .toList();

        when(eligibilityChecker.check(USER_ID, SNAPSHOT_AT)).thenReturn(
                new InsightEligibilityResponse(
                        true,
                        InsightEligibilityReason.AVAILABLE,
                        null,
                        10,
                        10,
                        10
                )
        );
        Job job = mock(Job.class);
        when(job.getId()).thenReturn(JOB_ID);
        when(job.getName()).thenReturn("백엔드 개발자");
        UserJob userJob = mock(UserJob.class);
        when(userJob.getJob()).thenReturn(job);
        when(userJobRepository.findByUserIdAndIsPrimaryTrue(USER_ID))
                .thenReturn(java.util.Optional.of(userJob));
        when(jobCompetencyRepository.findAllByJob_IdOrderBySortOrderAsc(JOB_ID))
                .thenReturn(competencies);
        when(insightProperties.embeddingModel()).thenReturn("gemini-embedding-2");
    }

    @Test
    void rejectsInsightBeforeAllCompetencyEmbeddingsAreReady() {
        UUID missingId = competencies.get(4).getId();
        for (int index = 0; index < 4; index++) {
            when(embeddingRepository.existsByJobCompetency_IdAndEmbeddingModel(
                    competencies.get(index).getId(),
                    "gemini-embedding-2"
            )).thenReturn(true);
        }
        when(embeddingRepository.existsByJobCompetency_IdAndEmbeddingModel(
                missingId,
                "gemini-embedding-2"
        )).thenReturn(false);

        assertThatThrownBy(() -> writer.createPending(USER_ID, SNAPSHOT_AT))
                .isInstanceOf(CustomException.class)
                .extracting(error -> ((CustomException) error).getErrorCode())
                .isEqualTo(JobErrorCode.JOB_COMPETENCY_EMBEDDING_NOT_READY);
        verify(insightRepository, never()).saveAndFlush(any());
    }

    @Test
    void createsInsightCommandAfterAllCompetencyEmbeddingsAreReady() {
        when(embeddingRepository.existsByJobCompetency_IdAndEmbeddingModel(
                any(UUID.class),
                eq("gemini-embedding-2")
        )).thenReturn(true);
        when(userRepository.findById(USER_ID))
                .thenReturn(java.util.Optional.of(mock(User.class)));
        Insight insight = mock(Insight.class);
        UUID insightId = UUID.randomUUID();
        when(insight.getId()).thenReturn(insightId);
        when(insightRepository.saveAndFlush(any(Insight.class)))
                .thenReturn(insight);

        InsightGenerationCommand command =
                writer.createPending(USER_ID, SNAPSHOT_AT);

        assertThat(command.insightId()).isEqualTo(insightId);
        assertThat(command.userId()).isEqualTo(USER_ID);
        assertThat(command.jobId()).isEqualTo(JOB_ID);
        assertThat(command.jobName()).isEqualTo("백엔드 개발자");
        assertThat(command.jobCompetencies()).hasSize(5);
        assertThat(command.jobCompetencies())
                .extracting(InsightGenerationCommand.JobCompetencySnapshot::jobCompetencyId)
                .containsExactlyElementsOf(
                        competencies.stream().map(JobCompetency::getId).toList()
                );
    }

    private JobCompetency competency(int index) {
        JobCompetency competency = mock(JobCompetency.class);
        CompetencyTag tag = mock(CompetencyTag.class);
        when(competency.getId()).thenReturn(UUID.randomUUID());
        lenient().when(competency.getCompetencyTag()).thenReturn(tag);
        lenient().when(tag.getName()).thenReturn("역량 " + index);
        lenient().when(tag.getDescription()).thenReturn("역량 설명 " + index);
        return competency;
    }
}
