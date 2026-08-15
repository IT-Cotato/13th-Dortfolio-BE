package com.itcotato.dortfolio.domain.job.embedding.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itcotato.dortfolio.domain.insight.config.InsightProperties;
import com.itcotato.dortfolio.domain.job.embedding.dto.EmbeddingRequest;
import com.itcotato.dortfolio.domain.job.embedding.dto.EmbeddingResponse;
import com.itcotato.dortfolio.domain.job.entity.Job;
import com.itcotato.dortfolio.domain.job.entity.JobCompetency;
import com.itcotato.dortfolio.domain.job.entity.JobCompetencyEmbedding;
import com.itcotato.dortfolio.domain.job.repository.JobCompetencyEmbeddingRepository;
import com.itcotato.dortfolio.domain.job.repository.JobCompetencyRepository;
import com.itcotato.dortfolio.domain.record.entity.CompetencyTag;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.JobErrorCode;
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
                insightProperties
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

        service.generate(competencyId);

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

        service.generate(competencyId);

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
    }

    private JobCompetency competency() {
        return JobCompetency.create(
                Job.create("TEST_JOB", "IT_DEVELOPMENT", "백엔드 개발자", null),
                CompetencyTag.create("TEST_COMP", "문제 해결", "문제의 원인을 파악합니다."),
                1
        );
    }
}
