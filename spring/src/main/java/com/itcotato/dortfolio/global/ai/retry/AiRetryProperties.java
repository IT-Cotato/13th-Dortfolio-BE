package com.itcotato.dortfolio.global.ai.retry;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "ai.retry")
public record AiRetryProperties(
        @DefaultValue("3") int maxAttempts,
        @DefaultValue("2s") Duration initialBackoff
) {
    public AiRetryProperties {
        if (maxAttempts < 1 || initialBackoff == null || initialBackoff.isNegative()) {
            throw new IllegalArgumentException("AI retry attempts must be positive and backoff must not be negative");
        }
    }
}
