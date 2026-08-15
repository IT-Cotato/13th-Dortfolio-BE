package com.itcotato.dortfolio.domain.job.embedding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itcotato.dortfolio.domain.job.entity.JobCompetencyEmbedding;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class JdbcJobCompetencyEmbeddingWriterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void returnsInsertedThenSkippedForDuplicateConflict() {
        when(jdbcTemplate.update(anyString(), any(Object[].class)))
                .thenReturn(1, 0);
        JdbcJobCompetencyEmbeddingWriter writer =
                new JdbcJobCompetencyEmbeddingWriter(jdbcTemplate);
        UUID jobCompetencyId = UUID.randomUUID();
        float[] embedding =
                new float[JobCompetencyEmbedding.EMBEDDING_DIMENSION];

        boolean first = writer.saveIfAbsent(
                jobCompetencyId,
                "gemini-embedding-2",
                embedding
        );
        boolean duplicate = writer.saveIfAbsent(
                jobCompetencyId,
                "gemini-embedding-2",
                embedding
        );

        assertThat(first).isTrue();
        assertThat(duplicate).isFalse();

        ArgumentCaptor<String> sqlCaptor =
                ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(2)).update(
                sqlCaptor.capture(),
                any(Object[].class)
        );
        assertThat(sqlCaptor.getAllValues())
                .allSatisfy(sql -> assertThat(sql)
                        .contains("on conflict")
                        .contains("job_competency_id")
                        .contains("embedding_model")
                        .contains("do nothing"));
    }
}
