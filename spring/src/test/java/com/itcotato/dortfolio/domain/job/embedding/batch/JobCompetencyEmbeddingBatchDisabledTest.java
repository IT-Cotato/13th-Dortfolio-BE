package com.itcotato.dortfolio.domain.job.embedding.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class JobCompetencyEmbeddingBatchDisabledTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void normalServerStartupDoesNotCreateBatchRunner() {
        assertThat(applicationContext.getBeansOfType(
                JobCompetencyEmbeddingBatchRunner.class
        )).isEmpty();
    }
}
