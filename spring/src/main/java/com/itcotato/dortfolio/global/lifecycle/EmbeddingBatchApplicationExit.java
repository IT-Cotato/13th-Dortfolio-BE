package com.itcotato.dortfolio.global.lifecycle;

import java.util.function.IntConsumer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EmbeddingBatchApplicationExit {

    static final String BATCH_ENABLED_PROPERTY =
            "job-competency-embedding.batch.enabled";

    public static void exitIfEnabled(
            ConfigurableApplicationContext context,
            IntConsumer processExit
    ) {
        boolean batchEnabled = context.getEnvironment().getProperty(
                BATCH_ENABLED_PROPERTY,
                Boolean.class,
                false
        );

        if (!batchEnabled) {
            return;
        }

        int exitCode = SpringApplication.exit(context);
        processExit.accept(exitCode);
    }
}
