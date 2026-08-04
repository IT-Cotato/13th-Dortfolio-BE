package com.itcotato.dortfolio.domain.job.embedding.service;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcJobCompetencyEmbeddingWriter
        implements JobCompetencyEmbeddingWriter {

    private final JdbcTemplate jdbcTemplate;

    public JdbcJobCompetencyEmbeddingWriter(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean saveIfAbsent(
            UUID jobCompetencyId,
            String embeddingModel,
            float[] embedding
    ) {
        int inserted = jdbcTemplate.update("""
                        insert into job_competency_embeddings (
                            id,
                            created_at,
                            updated_at,
                            job_competency_id,
                            embedding_model,
                            embedding
                        ) values (
                            ?,
                            current_timestamp,
                            current_timestamp,
                            ?,
                            ?,
                            cast(? as vector)
                        )
                        on conflict (
                            job_competency_id,
                            embedding_model
                        ) do nothing
                        """,
                UUID.randomUUID(),
                jobCompetencyId,
                embeddingModel,
                toVectorLiteral(embedding)
        );

        return inserted == 1;
    }

    private String toVectorLiteral(float[] embedding) {
        StringBuilder builder = new StringBuilder("[");

        for (int index = 0; index < embedding.length; index++) {
            if (index > 0) {
                builder.append(',');
            }

            builder.append(
                    Float.toString(embedding[index])
            );
        }

        return builder.append(']').toString();
    }
}