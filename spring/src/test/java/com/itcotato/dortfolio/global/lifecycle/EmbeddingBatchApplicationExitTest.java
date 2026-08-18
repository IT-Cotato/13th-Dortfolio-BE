package com.itcotato.dortfolio.global.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

class EmbeddingBatchApplicationExitTest {

    @Test
    void closesContextAndExitsSuccessfullyWhenJobCompetencyBatchIsEnabled() {
        AnnotationConfigApplicationContext context = applicationContext(
                EmbeddingBatchApplicationExit.JOB_COMPETENCY_BATCH_ENABLED_PROPERTY,
                true
        );
        AtomicInteger exitCode = new AtomicInteger(-1);

        EmbeddingBatchApplicationExit.exitIfEnabled(context, exitCode::set);

        assertThat(context.isActive()).isFalse();
        assertThat(exitCode).hasValue(0);
    }

    @Test
    void closesContextAndExitsSuccessfullyWhenStrengthTagBatchIsEnabled() {
        AnnotationConfigApplicationContext context = applicationContext(
                EmbeddingBatchApplicationExit.STRENGTH_TAG_BATCH_ENABLED_PROPERTY,
                true
        );
        AtomicInteger exitCode = new AtomicInteger(-1);

        EmbeddingBatchApplicationExit.exitIfEnabled(context, exitCode::set);

        assertThat(context.isActive()).isFalse();
        assertThat(exitCode).hasValue(0);
    }

    @Test
    void keepsApplicationRunningWhenBatchIsDisabled() {
        AnnotationConfigApplicationContext context = applicationContext(
                EmbeddingBatchApplicationExit.JOB_COMPETENCY_BATCH_ENABLED_PROPERTY,
                false
        );
        AtomicInteger exitCode = new AtomicInteger(-1);

        EmbeddingBatchApplicationExit.exitIfEnabled(context, exitCode::set);

        assertThat(context.isActive()).isTrue();
        assertThat(exitCode).hasValue(-1);

        context.close();
    }

    private AnnotationConfigApplicationContext applicationContext(
            String property,
            boolean batchEnabled
    ) {
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource(
                        "test",
                        java.util.Map.of(
                                property,
                                batchEnabled
                        )
                )
        );
        context.refresh();
        return context;
    }
}
