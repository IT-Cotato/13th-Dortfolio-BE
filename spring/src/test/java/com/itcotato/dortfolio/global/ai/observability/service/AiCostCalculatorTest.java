package com.itcotato.dortfolio.global.ai.observability.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.itcotato.dortfolio.global.ai.observability.config.AiPricingProperties;
import com.itcotato.dortfolio.global.ai.observability.config.AiPricingProperties.ModelRate;
import com.itcotato.dortfolio.global.ai.observability.dto.AiUsageResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiCostCalculatorTest {

    private final AiCostCalculator calculator = new AiCostCalculator(new AiPricingProperties(List.of(
            new ModelRate("GEMINI", "text-model", new BigDecimal("0.75"),
                    new BigDecimal("3.75"), null, null),
            new ModelRate("BEDROCK", "embedding-model", new BigDecimal("0.20"),
                    BigDecimal.ZERO, null, null)
    )));

    @Test
    void calculatesInputAndOutputWithBigDecimalPrecision() {
        AiUsageResponse usage = new AiUsageResponse(UUID.randomUUID(), "GEMINI", "text-model",
                1_000L, 200L, 25);

        assertThat(calculator.estimate(usage)).isEqualByComparingTo("0.0015000000");
    }

    @Test
    void embeddingDoesNotRequireOutputTokensWhenOutputRateIsZero() {
        AiUsageResponse usage = new AiUsageResponse(UUID.randomUUID(), "BEDROCK", "embedding-model",
                250L, null, 5);

        assertThat(calculator.estimate(usage)).isEqualByComparingTo("0.0000500000");
    }

    @Test
    void unregisteredModelAndMissingBillableTokensHaveUnknownCost() {
        assertThat(calculator.estimate(new AiUsageResponse(UUID.randomUUID(), "BEDROCK", "new-model",
                1_000L, 200L, 5))).isNull();
        assertThat(calculator.estimate(new AiUsageResponse(UUID.randomUUID(), "GEMINI", "text-model",
                1_000L, null, 5))).isNull();
        assertThat(calculator.estimate(new AiUsageResponse(UUID.randomUUID(), "GEMINI", "text-model",
                null, 200L, 5))).isNull();
    }
}
