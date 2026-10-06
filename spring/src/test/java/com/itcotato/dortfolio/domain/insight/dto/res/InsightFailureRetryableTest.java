package com.itcotato.dortfolio.domain.insight.dto.res;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.itcotato.dortfolio.domain.insight.entity.Insight;
import com.itcotato.dortfolio.domain.insight.entity.InsightGenerationStatus;
import org.junit.jupiter.api.Test;

class InsightFailureRetryableTest {
    @Test
    void onlyTemporaryFailedGenerationIsRetryable() {
        Insight insight = mock(Insight.class);
        when(insight.getStatus()).thenReturn(InsightGenerationStatus.FAILED);
        when(insight.getFailureCode()).thenReturn("I002");
        assertThat(InsightGenerationStatusResponse.from(insight).failureRetryable()).isTrue();
        for (String code : new String[]{"I003", "I010", "I011", "I012", "UNKNOWN_ERROR"}) {
            when(insight.getFailureCode()).thenReturn(code);
            assertThat(InsightGenerationStatusResponse.from(insight).failureRetryable()).isFalse();
        }
        when(insight.getFailureCode()).thenReturn("I002");
        when(insight.getStatus()).thenReturn(InsightGenerationStatus.RUNNING);
        assertThat(InsightGenerationStatusResponse.from(insight).failureRetryable()).isFalse();
    }
}
