package com.itcotato.dortfolio.domain.job.embedding.batch;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(
        prefix = "job-competency-embedding.batch"
)
public record JobCompetencyEmbeddingBatchProperties(
        boolean enabled,
        JobCompetencyEmbeddingBatchCommand command
) {

    public JobCompetencyEmbeddingBatchProperties {
        if (command == null) {
            command =
                    JobCompetencyEmbeddingBatchCommand.GENERATE_MISSING;
        }
    }
}
