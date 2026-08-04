package com.itcotato.dortfolio.domain.job.embedding.service;

import com.itcotato.dortfolio.domain.job.embedding.dto.EmbeddingRequest;
import com.itcotato.dortfolio.domain.job.embedding.dto.EmbeddingResponse;
import com.itcotato.dortfolio.domain.job.entity.JobCompetency;
import com.itcotato.dortfolio.domain.job.entity.JobCompetencyEmbedding;
import com.itcotato.dortfolio.domain.job.repository.JobCompetencyEmbeddingRepository;
import com.itcotato.dortfolio.domain.job.repository.JobCompetencyRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.JobErrorCode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobCompetencyEmbeddingService {

    private final JobCompetencyRepository
            jobCompetencyRepository;
    private final JobCompetencyEmbeddingRepository
            embeddingRepository;
    private final JobCompetencyEmbeddingTextBuilder
            textBuilder;
    private final EmbeddingClient embeddingClient;
    private final JobCompetencyEmbeddingWriter
            embeddingWriter;

    public void generate(UUID jobCompetencyId) {
        JobCompetency jobCompetency =
                jobCompetencyRepository
                        .findWithDetailsById(jobCompetencyId)
                        .orElseThrow(() -> new CustomException(
                                JobErrorCode.JOB_COMPETENCY_NOT_FOUND
                        ));

        String sourceText = textBuilder.build(jobCompetency);
        EmbeddingResponse response =
                requestEmbedding(
                        jobCompetencyId,
                        sourceText
                );

        validate(response);

        if (embeddingRepository
                .existsByJobCompetency_IdAndEmbeddingModel(
                        jobCompetencyId,
                        response.embeddingModel()
                )) {
            return;
        }

        embeddingWriter.saveIfAbsent(
                jobCompetencyId,
                response.embeddingModel(),
                response.embedding()
        );
    }

    public void generateAllMissing() {
        List<JobCompetency> jobCompetencies =
                jobCompetencyRepository
                        .findAllByOrderByJob_IdAscSortOrderAsc();

        for (JobCompetency jobCompetency : jobCompetencies) {
            generate(jobCompetency.getId());
        }
    }

    private EmbeddingResponse requestEmbedding(
            UUID jobCompetencyId,
            String sourceText
    ) {
        try {
            return embeddingClient.embed(
                    new EmbeddingRequest(sourceText)
            );
        } catch (RestClientException exception) {
            log.warn(
                    "Job competency embedding request failed. "
                            + "jobCompetencyId={}",
                    jobCompetencyId,
                    exception
            );

            throw new CustomException(
                    JobErrorCode.JOB_COMPETENCY_EMBEDDING_AI_SERVICE_FAILED
            );
        }
    }

    private void validate(EmbeddingResponse response) {
        if (response == null
                || !StringUtils.hasText(
                response.embeddingModel()
        )
                || response.embedding() == null
                || response.embedding().length
                != JobCompetencyEmbedding.EMBEDDING_DIMENSION) {
            throw new CustomException(
                    JobErrorCode.JOB_COMPETENCY_EMBEDDING_INVALID_RESPONSE
            );
        }

        for (float value : response.embedding()) {
            if (Float.isNaN(value)
                    || Float.isInfinite(value)) {
                throw new CustomException(
                        JobErrorCode.JOB_COMPETENCY_EMBEDDING_INVALID_RESPONSE
                );
            }
        }
    }
}
