package com.itcotato.dortfolio.domain.insight.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itcotato.dortfolio.domain.insight.dto.res.InsightEligibilityReason;
import com.itcotato.dortfolio.domain.insight.dto.res.InsightEligibilityResponse;
import com.itcotato.dortfolio.domain.insight.dto.res.InsightGenerationStatusResponse;
import com.itcotato.dortfolio.domain.insight.entity.InsightGenerationStatus;
import com.itcotato.dortfolio.domain.insight.generation.model.InsightGenerationStartResult;
import com.itcotato.dortfolio.domain.insight.generation.service.InsightGenerationService;
import com.itcotato.dortfolio.domain.insight.service.InsightEligibilityService;
import com.itcotato.dortfolio.domain.insight.service.InsightGenerationQueryService;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class InsightControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID GENERATION_ID = UUID.randomUUID();

    @Mock
    private InsightEligibilityService eligibilityService;

    @Mock
    private InsightGenerationService generationService;

    @Mock
    private InsightGenerationQueryService generationQueryService;

    private InsightController controller;

    @BeforeEach
    void setUp() {
        controller = new InsightController(
                eligibilityService,
                generationService,
                generationQueryService
        );
    }

    @Test
    void returnsEligibilityForAuthenticatedUser() {
        // given
        when(eligibilityService.check(USER_ID))
                .thenReturn(new InsightEligibilityResponse(
                        true,
                        InsightEligibilityReason.AVAILABLE,
                        null,
                        13,
                        12,
                        10
                ));

        // when
        var response = controller.getEligibility(USER_ID);

        // then
        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().eligible()).isTrue();
        assertThat(response.getBody().getData().currentRecordCount())
                .isEqualTo(12);
        assertThat(response.getBody().getData().requiredRecordCount())
                .isEqualTo(10);
        verify(eligibilityService).check(USER_ID);
    }

    @Test
    void acceptsGenerationRequestAndReturnsPending() {
        // given
        when(generationService.requestGeneration(USER_ID))
                .thenReturn(InsightGenerationStartResult.pending(
                        GENERATION_ID
                ));

        // when
        var response = controller.createInsight(USER_ID);

        // then
        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().generationId())
                .isEqualTo(GENERATION_ID);
        assertThat(response.getBody().getData().status())
                .isEqualTo(InsightGenerationStatus.PENDING);
        verify(generationService).requestGeneration(USER_ID);
    }

    @Test
    void returnsGenerationStatusForAuthenticatedUser() {
        // given
        LocalDateTime requestedAt =
                LocalDateTime.of(2026, 8, 6, 16, 30);
        LocalDateTime failedAt = requestedAt.plusSeconds(7);

        InsightGenerationStatusResponse statusResponse =
                new InsightGenerationStatusResponse(
                        GENERATION_ID,
                        InsightGenerationStatus.FAILED,
                        requestedAt,
                        null,
                        failedAt,
                        "I002",
                        "Insight recommendation service failed."
                );

        when(generationQueryService.getStatus(
                USER_ID,
                GENERATION_ID
        )).thenReturn(statusResponse);

        // when
        var response = controller.getGenerationStatus(
                USER_ID,
                GENERATION_ID
        );

        // then
        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData())
                .isEqualTo(statusResponse);
        verify(generationQueryService).getStatus(
                USER_ID,
                GENERATION_ID
        );
    }
}
