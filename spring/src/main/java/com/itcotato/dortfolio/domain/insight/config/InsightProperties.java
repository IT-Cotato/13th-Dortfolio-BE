package com.itcotato.dortfolio.domain.insight.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "insight")
public record InsightProperties(
        int minimumAnalyzedRecordCount,
        Duration regenerationCooldown
) {
    public InsightProperties {
        if (minimumAnalyzedRecordCount <= 0) {
            throw new IllegalArgumentException(
                    "minimumAnalyzedRecordCount must be positive"
            );
        }
        if (regenerationCooldown == null
                || regenerationCooldown.isZero()
                || regenerationCooldown.isNegative()) {
            throw new IllegalArgumentException(
                    "regenerationCooldown must be positive"
            );
        }
    }
}
