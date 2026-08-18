package com.itcotato.dortfolio.domain.job.embedding.batch;

import com.itcotato.dortfolio.domain.job.embedding.model.JobCompetencyEmbeddingBatchResult;
import com.itcotato.dortfolio.domain.job.embedding.model.JobCompetencyEmbeddingCoverage;
import com.itcotato.dortfolio.domain.job.embedding.service.JobCompetencyEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "job-competency-embedding.batch",
        name = "enabled",
        havingValue = "true"
)
public class JobCompetencyEmbeddingBatchRunner
        implements ApplicationRunner {

    private final JobCompetencyEmbeddingService embeddingService;
    private final JobCompetencyEmbeddingBatchProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        if (properties.command()
                == JobCompetencyEmbeddingBatchCommand.VERIFY) {
            verifyComplete();
            return;
        }

        JobCompetencyEmbeddingBatchResult result =
                embeddingService.generateAllMissing();

        JobCompetencyEmbeddingCoverage coverage =
                embeddingService.inspectCoverage();

        log.info(
                "Job competency embedding command completed. "
                        + "total={}, generated={}, skipped={}, failed={}, "
                        + "embedded={}, missing={}",
                result.totalCount(),
                result.generatedCount(),
                result.skippedCount(),
                result.failedCount(),
                coverage.embeddedCount(),
                coverage.missingCount()
        );

        if (result.hasFailures() || !coverage.isComplete()) {
            throw new IllegalStateException(
                    "직무 역량 임베딩 생성이 완료되지 않았습니다. "
                            + "failed=" + result.failedCount()
                            + ", missing=" + coverage.missingCount()
            );
        }
    }

    private void verifyComplete() {
        JobCompetencyEmbeddingCoverage coverage =
                embeddingService.inspectCoverage();

        log.info(
                "Job competency embedding coverage. "
                        + "model={}, total={}, embedded={}, missing={}, "
                        + "missingIds={}",
                coverage.embeddingModel(),
                coverage.totalCount(),
                coverage.embeddedCount(),
                coverage.missingCount(),
                coverage.missingJobCompetencyIds()
        );

        if (!coverage.isComplete()) {
            throw new IllegalStateException(
                    "누락된 직무 역량 임베딩이 있습니다. missing="
                            + coverage.missingCount()
            );
        }
    }
}
