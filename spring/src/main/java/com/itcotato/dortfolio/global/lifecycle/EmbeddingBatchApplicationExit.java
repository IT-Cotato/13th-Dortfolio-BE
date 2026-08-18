package com.itcotato.dortfolio.global.lifecycle;

import java.util.function.IntConsumer;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EmbeddingBatchApplicationExit {

    static final String JOB_COMPETENCY_BATCH_ENABLED_PROPERTY =
            "job-competency-embedding.batch.enabled";
    static final String STRENGTH_TAG_BATCH_ENABLED_PROPERTY =
            "strength-tag-embedding.batch.enabled";

    public static void exitIfEnabled(
            ConfigurableApplicationContext context,
            IntConsumer processExit
    ) {
        boolean batchEnabled = List.of(
                        JOB_COMPETENCY_BATCH_ENABLED_PROPERTY,
                        STRENGTH_TAG_BATCH_ENABLED_PROPERTY
                ).stream()
                .anyMatch(property -> context.getEnvironment().getProperty(
                        property,
                        Boolean.class,
                        false
                ));

        if (!batchEnabled) {
            return;
        }

        int exitCode = SpringApplication.exit(context);
        processExit.accept(exitCode);
    }
}
