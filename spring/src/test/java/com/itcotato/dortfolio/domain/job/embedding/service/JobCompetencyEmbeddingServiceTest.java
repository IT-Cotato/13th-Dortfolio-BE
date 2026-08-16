package com.itcotato.dortfolio.domain.job.embedding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itcotato.dortfolio.domain.insight.config.InsightProperties;
import com.itcotato.dortfolio.domain.job.embedding.dto.EmbeddingRequest;
import com.itcotato.dortfolio.domain.job.embedding.dto.EmbeddingResponse;
import com.itcotato.dortfolio.domain.job.embedding.model.JobCompetencyEmbeddingBatchResult;
import com.itcotato.dortfolio.domain.job.embedding.model.JobCompetencyEmbeddingCoverage;
import com.itcotato.dortfolio.domain.job.embedding.model.JobCompetencyEmbeddingGenerationStatus;
import com.itcotato.dortfolio.domain.job.entity.Job;
import com.itcotato.dortfolio.domain.job.entity.JobCompetency;
import com.itcotato.dortfolio.domain.job.entity.JobCompetencyEmbedding;
import com.itcotato.dortfolio.domain.job.repository.JobCompetencyEmbeddingRepository;
import com.itcotato.dortfolio.domain.job.repository.JobCompetencyRepository;
import com.itcotato.dortfolio.domain.record.entity.CompetencyTag;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.JobErrorCode;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

@ExtendWith(MockitoExtension.class)
class JobCompetencyEmbeddingServiceTest {

    @Mock
    private JobCompetencyRepository jobCompetencyRepository;
    @Mock
    private JobCompetencyEmbeddingRepository embeddingRepository;
    @Mock
    private EmbeddingClient embeddingClient;
    @Mock
    private JobCompetencyEmbeddingWriter embeddingWriter;

    @Mock
    private InsightProperties insightProperties;

    private JobCompetencyEmbeddingService service;

    @BeforeEach
    void setUp() {
        service = new JobCompetencyEmbeddingService(
                jobCompetencyRepository,
                embeddingRepository,
                new JobCompetencyEmbeddingTextBuilder(),
                embeddingClient,
                embeddingWriter,
                insightProperties,
                new JobCompetencyEmbeddingRequestProperties(
                        Duration.ZERO,
                        3,
                        Duration.ZERO
                )
        );
    }

    @Test
    void generatesAndStoresEmbedding() {
        when(insightProperties.embeddingModel()).thenReturn("gemini-embedding-2");

        UUID competencyId = UUID.randomUUID();
        JobCompetency competency = competency();
        float[] vector = new float[JobCompetencyEmbedding.EMBEDDING_DIMENSION];
        EmbeddingRequest request = new EmbeddingRequest("""
                직무: 백엔드 개발자
                역량: 문제 해결
                설명: 문제의 원인을 파악합니다.""");

        when(jobCompetencyRepository.findWithDetailsById(competencyId))
                .thenReturn(Optional.of(competency));
        when(embeddingClient.embed(request))
                .thenReturn(new EmbeddingResponse("gemini-embedding-2", vector));
        when(embeddingWriter.saveIfAbsent(
                competencyId,
                "gemini-embedding-2",
                vector
        )).thenReturn(true);

        JobCompetencyEmbeddingGenerationStatus result =
                service.generate(competencyId);

        assertThat(result)
                .isEqualTo(JobCompetencyEmbeddingGenerationStatus.GENERATED);
        verify(embeddingWriter).saveIfAbsent(
                competencyId,
                "gemini-embedding-2",
                vector
        );
    }

    @Test
    void skipsStoreWhenSameModelAlreadyExists() {
        UUID competencyId = UUID.randomUUID();
        when(insightProperties.embeddingModel()).thenReturn("gemini-embedding-2");
        when(embeddingRepository.existsByJobCompetency_IdAndEmbeddingModel(
                competencyId,
                "gemini-embedding-2"
        )).thenReturn(true);

        JobCompetencyEmbeddingGenerationStatus result =
                service.generate(competencyId);

        assertThat(result)
                .isEqualTo(JobCompetencyEmbeddingGenerationStatus.SKIPPED);
        verify(embeddingClient, never()).embed(any());
        verify(jobCompetencyRepository, never()).findWithDetailsById(any());
        verify(embeddingWriter, never()).saveIfAbsent(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void rejectsWrongEmbeddingDimensionWithCommonErrorCode() {
        when(insightProperties.embeddingModel()).thenReturn("gemini-embedding-2");

        UUID competencyId = UUID.randomUUID();
        when(jobCompetencyRepository.findWithDetailsById(competencyId))
                .thenReturn(Optional.of(competency()));
        when(embeddingClient.embed(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new EmbeddingResponse("gemini-embedding-2", new float[2]));

        assertThatThrownBy(() -> service.generate(competencyId))
                .isInstanceOf(CustomException.class)
                .extracting(error -> ((CustomException) error).getErrorCode())
                .isEqualTo(JobErrorCode.JOB_COMPETENCY_EMBEDDING_INVALID_RESPONSE);
    }

    @Test
    void convertsAiClientFailureToCommonErrorCode() {
        when(insightProperties.embeddingModel()).thenReturn("gemini-embedding-2");

        UUID competencyId = UUID.randomUUID();
        when(jobCompetencyRepository.findWithDetailsById(competencyId))
                .thenReturn(Optional.of(competency()));
        when(embeddingClient.embed(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new RestClientException("timeout"));

        assertThatThrownBy(() -> service.generate(competencyId))
                .isInstanceOf(CustomException.class)
                .extracting(error -> ((CustomException) error).getErrorCode())
                .isEqualTo(JobErrorCode.JOB_COMPETENCY_EMBEDDING_AI_SERVICE_FAILED);
        verify(embeddingClient, times(3)).embed(any());
    }

    @Test
    void retriesTransientAiClientFailure() {
        when(insightProperties.embeddingModel()).thenReturn("gemini-embedding-2");

        UUID competencyId = UUID.randomUUID();
        float[] vector = embedding();
        when(jobCompetencyRepository.findWithDetailsById(competencyId))
                .thenReturn(Optional.of(competency()));
        when(embeddingClient.embed(any()))
                .thenThrow(new RestClientException("temporary failure"))
                .thenReturn(new EmbeddingResponse("gemini-embedding-2", vector));
        when(embeddingWriter.saveIfAbsent(
                competencyId,
                "gemini-embedding-2",
                vector
        )).thenReturn(true);

        JobCompetencyEmbeddingGenerationStatus result =
                service.generate(competencyId);

        assertThat(result)
                .isEqualTo(JobCompetencyEmbeddingGenerationStatus.GENERATED);
        verify(embeddingClient, times(2)).embed(any());
    }

    @Test
    void reportsSkippedWhenConcurrentExecutionStoresFirst() {
        UUID competencyId = UUID.randomUUID();
        float[] vector = embedding();
        when(insightProperties.embeddingModel()).thenReturn("gemini-embedding-2");
        when(jobCompetencyRepository.findWithDetailsById(competencyId))
                .thenReturn(Optional.of(competency()));
        when(embeddingClient.embed(any()))
                .thenReturn(new EmbeddingResponse("gemini-embedding-2", vector));
        when(embeddingWriter.saveIfAbsent(
                competencyId,
                "gemini-embedding-2",
                vector
        )).thenReturn(false);

        JobCompetencyEmbeddingGenerationStatus result =
                service.generate(competencyId);

        assertThat(result)
                .isEqualTo(JobCompetencyEmbeddingGenerationStatus.SKIPPED);
    }

    @Test
    void generatesOnlyMissingItemsAndAggregatesResult() {
        UUID firstMissingId = UUID.randomUUID();
        UUID secondMissingId = UUID.randomUUID();
        when(insightProperties.embeddingModel()).thenReturn("gemini-embedding-2");
        when(jobCompetencyRepository.count()).thenReturn(5L);
        when(jobCompetencyRepository.findMissingEmbeddingIdsByModel("gemini-embedding-2"))
                .thenReturn(List.of(firstMissingId, secondMissingId));
        when(jobCompetencyRepository.findWithDetailsById(any()))
                .thenReturn(Optional.of(competency()));
        when(embeddingClient.embed(any()))
                .thenReturn(new EmbeddingResponse("gemini-embedding-2", embedding()));
        when(embeddingWriter.saveIfAbsent(any(), any(), any()))
                .thenReturn(true);

        JobCompetencyEmbeddingBatchResult result = service.generateAllMissing();

        assertThat(result.totalCount()).isEqualTo(5);
        assertThat(result.generatedCount()).isEqualTo(2);
        assertThat(result.skippedCount()).isEqualTo(3);
        assertThat(result.failedCount()).isZero();
        assertThat(result.failures()).isEmpty();
        verify(embeddingClient, times(2)).embed(any());
        verify(embeddingWriter).saveIfAbsent(
                org.mockito.ArgumentMatchers.eq(firstMissingId), any(), any()
        );
        verify(embeddingWriter).saveIfAbsent(
                org.mockito.ArgumentMatchers.eq(secondMissingId), any(), any()
        );
    }

    @Test
    void continuesAfterOneItemFailsAndRecordsFailure() {
        UUID firstId = UUID.randomUUID();
        UUID failedId = UUID.randomUUID();
        UUID lastId = UUID.randomUUID();
        when(insightProperties.embeddingModel()).thenReturn("gemini-embedding-2");
        when(jobCompetencyRepository.count()).thenReturn(3L);
        when(jobCompetencyRepository.findMissingEmbeddingIdsByModel("gemini-embedding-2"))
                .thenReturn(List.of(firstId, failedId, lastId));
        when(jobCompetencyRepository.findWithDetailsById(any()))
                .thenReturn(Optional.of(competency()));
        when(embeddingClient.embed(any()))
                .thenReturn(new EmbeddingResponse("gemini-embedding-2", embedding()));
        when(embeddingWriter.saveIfAbsent(any(), any(), any()))
                .thenReturn(true)
                .thenThrow(new IllegalStateException("database unavailable"))
                .thenReturn(true);

        JobCompetencyEmbeddingBatchResult result = service.generateAllMissing();

        assertThat(result.generatedCount()).isEqualTo(2);
        assertThat(result.skippedCount()).isZero();
        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.failures()).singleElement().satisfies(failure -> {
            assertThat(failure.jobCompetencyId()).isEqualTo(failedId);
            assertThat(failure.reason()).isEqualTo("database unavailable");
        });
        verify(embeddingWriter).saveIfAbsent(
                org.mockito.ArgumentMatchers.eq(lastId), any(), any()
        );
    }

    @Test
    void retriesOnlyStillMissingItemWithoutDuplicatingSuccessfulItem() {
        UUID completedId = UUID.randomUUID();
        UUID retryId = UUID.randomUUID();
        when(insightProperties.embeddingModel()).thenReturn("gemini-embedding-2");
        when(jobCompetencyRepository.count()).thenReturn(2L);
        when(jobCompetencyRepository.findMissingEmbeddingIdsByModel("gemini-embedding-2"))
                .thenReturn(List.of(completedId, retryId), List.of(retryId));
        when(jobCompetencyRepository.findWithDetailsById(any()))
                .thenReturn(Optional.of(competency()));
        when(embeddingClient.embed(any()))
                .thenReturn(new EmbeddingResponse("gemini-embedding-2", embedding()));
        when(embeddingWriter.saveIfAbsent(any(), any(), any()))
                .thenReturn(true)
                .thenThrow(new IllegalStateException("temporary failure"))
                .thenReturn(true);

        JobCompetencyEmbeddingBatchResult first = service.generateAllMissing();
        JobCompetencyEmbeddingBatchResult second = service.generateAllMissing();

        assertThat(first.generatedCount()).isEqualTo(1);
        assertThat(first.failedCount()).isEqualTo(1);
        assertThat(second.generatedCount()).isEqualTo(1);
        assertThat(second.skippedCount()).isEqualTo(1);
        assertThat(second.failedCount()).isZero();
        verify(embeddingWriter, times(1)).saveIfAbsent(
                org.mockito.ArgumentMatchers.eq(completedId), any(), any()
        );
        verify(embeddingWriter, times(2)).saveIfAbsent(
                org.mockito.ArgumentMatchers.eq(retryId), any(), any()
        );
    }

    @Test
    void inspectsCoverageForCurrentModel() {
        UUID missingId = UUID.randomUUID();
        when(insightProperties.embeddingModel()).thenReturn("gemini-embedding-2");
        when(jobCompetencyRepository.count()).thenReturn(5L);
        when(embeddingRepository.countByEmbeddingModel("gemini-embedding-2"))
                .thenReturn(4L);
        when(jobCompetencyRepository.findMissingEmbeddingIdsByModel("gemini-embedding-2"))
                .thenReturn(List.of(missingId));

        JobCompetencyEmbeddingCoverage coverage = service.inspectCoverage();

        assertThat(coverage.embeddingModel()).isEqualTo("gemini-embedding-2");
        assertThat(coverage.totalCount()).isEqualTo(5);
        assertThat(coverage.embeddedCount()).isEqualTo(4);
        assertThat(coverage.missingCount()).isEqualTo(1);
        assertThat(coverage.missingJobCompetencyIds()).containsExactly(missingId);
        assertThat(coverage.isComplete()).isFalse();
    }

    private float[] embedding() {
        return new float[JobCompetencyEmbedding.EMBEDDING_DIMENSION];
    }

    private JobCompetency competency() {
        return JobCompetency.create(
                Job.create("TEST_JOB", "IT_DEVELOPMENT", "백엔드 개발자", null),
                CompetencyTag.create("TEST_COMP", "문제 해결", "문제의 원인을 파악합니다."),
                1
        );
    }
}
