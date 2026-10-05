package com.itcotato.dortfolio.global.ai.observability.service;

import com.itcotato.dortfolio.global.ai.observability.config.AiPricingProperties;
import com.itcotato.dortfolio.global.ai.observability.config.AiPricingProperties.ModelRate;
import com.itcotato.dortfolio.global.ai.observability.dto.AiUsageResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
public class AiCostCalculator {

    private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000L);
    private final AiPricingProperties pricingProperties;

    public AiCostCalculator(AiPricingProperties pricingProperties) {
        this.pricingProperties = pricingProperties;
    }

    public BigDecimal estimate(AiUsageResponse usage) {
        if (usage == null || usage.provider() == null || usage.modelId() == null) {
            return null;
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        ModelRate matchingRate = null;
        for (ModelRate rate : pricingProperties.rates()) {
            if (rate.appliesTo(usage.provider(), usage.modelId(), today)) {
                if (matchingRate != null) {
                    throw new IllegalStateException("Overlapping AI pricing rates for model: " + usage.modelId());
                }
                matchingRate = rate;
            }
        }
        if (matchingRate == null || usage.inputTokens() == null || usage.inputTokens() < 0) {
            return null;
        }
        if (usage.outputTokens() == null && matchingRate.outputUsdPerMillionTokens().signum() != 0) {
            return null;
        }
        if (usage.outputTokens() != null && usage.outputTokens() < 0) {
            return null;
        }

        BigDecimal inputCost = BigDecimal.valueOf(usage.inputTokens())
                .multiply(matchingRate.inputUsdPerMillionTokens());
        BigDecimal outputCost = BigDecimal.valueOf(
                usage.outputTokens() == null ? 0L : usage.outputTokens())
                .multiply(matchingRate.outputUsdPerMillionTokens());
        return inputCost.add(outputCost).divide(ONE_MILLION, 10, RoundingMode.HALF_UP);
    }
}
