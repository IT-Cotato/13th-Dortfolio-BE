package com.itcotato.dortfolio.domain.insight.generation.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "insight.generation")
public record InsightGenerationProperties(
        Duration lockTtl,
        int asyncCorePoolSize,
        int asyncMaxPoolSize,
        int asyncQueueCapacity
) {

    public InsightGenerationProperties {
        if (lockTtl == null
                || lockTtl.isZero()
                || lockTtl.isNegative()) {
            throw new IllegalArgumentException(
                    "lockTtl must be positive"
            );
        }

        if (asyncCorePoolSize <= 0) {
            throw new IllegalArgumentException(
                    "asyncCorePoolSize must be positive"
            );
        }

        if (asyncMaxPoolSize < asyncCorePoolSize) {
            throw new IllegalArgumentException(
                    "asyncMaxPoolSize must be greater than or equal to core size"
            );
        }

        if (asyncQueueCapacity <= 0) {
            throw new IllegalArgumentException(
                    "asyncQueueCapacity must be positive"
            );
        }
    }
}