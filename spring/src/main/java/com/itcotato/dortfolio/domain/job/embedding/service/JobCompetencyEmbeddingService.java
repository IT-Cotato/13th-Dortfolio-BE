package com.itcotato.dortfolio.domain.job.embedding.service;

import com.itcotato.dortfolio.domain.insight.config.InsightProperties;
import com.itcotato.dortfolio.domain.job.embedding.dto.EmbeddingRequest;
import com.itcotato.dortfolio.domain.job.embedding.dto.EmbeddingResponse;
import com.itcotato.dortfolio.domain.job.embedding.model.JobCompetencyEmbeddingBatchResult;
import com.itcotato.dortfolio.domain.job.embedding.model.JobCompetencyEmbeddingCoverage;
import com.itcotato.dortfolio.domain.job.embedding.model.JobCompetencyEmbeddingFailure;
import com.itcotato.dortfolio.domain.job.embedding.model.JobCompetencyEmbeddingGenerationStatus;
import com.itcotato.dortfolio.domain.job.entity.JobCompetency;
import com.itcotato.dortfolio.domain.job.entity.JobCompetencyEmbedding;
import com.itcotato.dortfolio.domain.job.repository.JobCompetencyEmbeddingRepository;
import com.itcotato.dortfolio.domain.job.repository.JobCompetencyRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.JobErrorCode;
import java.time.Duration;
import java.util.ArrayList;
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

    private final JobCompetencyRepository jobCompetencyRepository;
    private final JobCompetencyEmbeddingRepository embeddingRepository;
    private final JobCompetencyEmbeddingTextBuilder textBuilder;
    private final EmbeddingClient embeddingClient;
    private final JobCompetencyEmbeddingWriter embeddingWriter;
    private final InsightProperties insightProperties;
    private final JobCompetencyEmbeddingRequestProperties requestProperties;

    public JobCompetencyEmbeddingGenerationStatus generate(UUID jobCompetencyId) {
        String targetModel = insightProperties.embeddingModel();

        // 외부 API 호출 전에 DB에 해당 모델의 임베딩이 존재하는지 확인
        if (embeddingRepository.existsByJobCompetency_IdAndEmbeddingModel(
                jobCompetencyId,
                targetModel
        )) {
            return JobCompetencyEmbeddingGenerationStatus.SKIPPED;
        }

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

        // 응답 검증 (targetModel과 일치하는지도 검증)
        validate(response, targetModel);

        boolean inserted = embeddingWriter.saveIfAbsent(
                jobCompetencyId,
                response.embeddingModel(),
                response.embedding()
        );

        return inserted
                ? JobCompetencyEmbeddingGenerationStatus.GENERATED
                : JobCompetencyEmbeddingGenerationStatus.SKIPPED;
    }

    public JobCompetencyEmbeddingBatchResult generateAllMissing() {
        String targetModel = insightProperties.embeddingModel();

        long totalCount = jobCompetencyRepository.count();
        List<UUID> missingIds =
                jobCompetencyRepository.findMissingEmbeddingIdsByModel(targetModel);

        long generatedCount = 0;
        long skippedCount = totalCount - missingIds.size();
        List<JobCompetencyEmbeddingFailure> failures = new ArrayList<>();

        for (int index = 0; index < missingIds.size(); index++) {
            UUID jobCompetencyId = missingIds.get(index);

            try {
                if (index > 0) {
                    pause(requestProperties.interval());
                }

                JobCompetencyEmbeddingGenerationStatus status =
                        generate(jobCompetencyId);

                if (status
                        == JobCompetencyEmbeddingGenerationStatus.GENERATED) {
                    generatedCount++;
                } else {
                    skippedCount++;
                }
            } catch (Exception exception) {
                String reason = toFailureReason(exception);

                failures.add(new JobCompetencyEmbeddingFailure(
                        jobCompetencyId,
                        reason
                ));

                log.error(
                        "Job competency embedding generation failed. "
                                + "jobCompetencyId={}, reason={}",
                        jobCompetencyId,
                        reason,
                        exception
                );
            }
        }

        JobCompetencyEmbeddingBatchResult result =
                new JobCompetencyEmbeddingBatchResult(
                        totalCount,
                        generatedCount,
                        skippedCount,
                        failures.size(),
                        failures
                );

        log.info(
                "Job competency embedding batch completed. "
                        + "model={}, total={}, generated={}, skipped={}, failed={}",
                targetModel,
                result.totalCount(),
                result.generatedCount(),
                result.skippedCount(),
                result.failedCount()
        );

        return result;
    }

    public JobCompetencyEmbeddingCoverage inspectCoverage() {
        String targetModel = insightProperties.embeddingModel();

        long totalCount = jobCompetencyRepository.count();
        long embeddedCount =
                embeddingRepository.countByEmbeddingModel(targetModel);

        List<UUID> missingIds =
                jobCompetencyRepository.findMissingEmbeddingIdsByModel(
                        targetModel
                );

        return new JobCompetencyEmbeddingCoverage(
                targetModel,
                totalCount,
                embeddedCount,
                missingIds
        );
    }

    public JobCompetencyEmbeddingCoverage verifyComplete() {
        JobCompetencyEmbeddingCoverage coverage = inspectCoverage();

        if (!coverage.isComplete()) {
            log.error(
                    "Job competency embedding coverage incomplete. "
                            + "model={}, total={}, embedded={}, missing={}, "
                            + "missingIds={}",
                    coverage.embeddingModel(),
                    coverage.totalCount(),
                    coverage.embeddedCount(),
                    coverage.missingCount(),
                    coverage.missingJobCompetencyIds()
            );
        }

        return coverage;
    }

    private String toFailureReason(Exception exception) {
        if (exception instanceof CustomException customException) {
            return customException.getErrorCode().getCode()
                    + " "
                    + customException.getErrorCode().getMessage();
        }

        return exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }

    private EmbeddingResponse requestEmbedding(
            UUID jobCompetencyId,
            String sourceText
    ) {
        EmbeddingRequest request = new EmbeddingRequest(sourceText);

        for (int attempt = 1; attempt <= requestProperties.maxAttempts(); attempt++) {
            try {
                return embeddingClient.embed(request);
            } catch (RestClientException exception) {
                if (attempt == requestProperties.maxAttempts()) {
                    log.warn(
                            "Job competency embedding request failed after retries. "
                                    + "jobCompetencyId={}, attempts={}",
                            jobCompetencyId,
                            attempt,
                            exception
                    );

                    throw new CustomException(
                            JobErrorCode.JOB_COMPETENCY_EMBEDDING_AI_SERVICE_FAILED
                    );
                }

                Duration backoff = requestProperties.initialBackoff()
                        .multipliedBy(1L << Math.min(attempt - 1, 10));

                log.warn(
                        "Job competency embedding request failed; retrying. "
                                + "jobCompetencyId={}, attempt={}, maxAttempts={}, "
                                + "backoff={}",
                        jobCompetencyId,
                        attempt,
                        requestProperties.maxAttempts(),
                        backoff
                );
                pause(backoff);
            }
        }

        throw new IllegalStateException("Embedding retry loop completed unexpectedly");
    }

    private void pause(Duration duration) {
        if (duration.isZero()) {
            return;
        }

        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CustomException(
                    JobErrorCode.JOB_COMPETENCY_EMBEDDING_AI_SERVICE_FAILED
            );
        }
    }

    private void validate(EmbeddingResponse response, String targetModel) {
        if (response == null
                || !StringUtils.hasText(response.embeddingModel())
                || !targetModel.equals(response.embeddingModel())
                || response.embedding() == null
                || response.embedding().length != JobCompetencyEmbedding.EMBEDDING_DIMENSION) {
            throw new CustomException(
                    JobErrorCode.JOB_COMPETENCY_EMBEDDING_INVALID_RESPONSE
            );
        }

        for (float value : response.embedding()) {
            if (Float.isNaN(value) || Float.isInfinite(value)) {
                throw new CustomException(
                        JobErrorCode.JOB_COMPETENCY_EMBEDDING_INVALID_RESPONSE
                );
            }
        }
    }
}
