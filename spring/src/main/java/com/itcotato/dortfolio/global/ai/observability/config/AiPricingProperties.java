package com.itcotato.dortfolio.global.ai.observability.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.pricing")
public record AiPricingProperties(List<ModelRate> rates) {

    public AiPricingProperties {
        rates = rates == null ? List.of() : List.copyOf(rates);
    }

    public record ModelRate(
            String provider,
            String modelId,
            BigDecimal inputUsdPerMillionTokens,
            BigDecimal outputUsdPerMillionTokens,
            LocalDate effectiveFrom,
            LocalDate effectiveThrough
    ) {
        public ModelRate {
            if (provider == null || provider.isBlank() || modelId == null || modelId.isBlank()) {
                throw new IllegalArgumentException("AI pricing provider and modelId are required.");
            }
            if (inputUsdPerMillionTokens == null || inputUsdPerMillionTokens.signum() < 0
                    || outputUsdPerMillionTokens == null || outputUsdPerMillionTokens.signum() < 0) {
                throw new IllegalArgumentException("AI pricing token rates must be non-negative.");
            }
            if (effectiveFrom != null && effectiveThrough != null
                    && effectiveThrough.isBefore(effectiveFrom)) {
                throw new IllegalArgumentException("AI pricing effective date range is invalid.");
            }
        }

        public boolean appliesTo(String requestedProvider, String requestedModelId, LocalDate date) {
            return provider.equalsIgnoreCase(requestedProvider)
                    && modelId.equals(requestedModelId)
                    && (effectiveFrom == null || !date.isBefore(effectiveFrom))
                    && (effectiveThrough == null || !date.isAfter(effectiveThrough));
        }
    }
}
