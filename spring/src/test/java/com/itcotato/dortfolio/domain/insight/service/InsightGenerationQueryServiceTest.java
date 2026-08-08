package com.itcotato.dortfolio.domain.insight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.itcotato.dortfolio.domain.insight.dto.res.InsightGenerationStatusResponse;
import com.itcotato.dortfolio.domain.insight.entity.Insight;
import com.itcotato.dortfolio.domain.insight.entity.InsightGenerationStatus;
import com.itcotato.dortfolio.domain.insight.repository.InsightRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.InsightErrorCode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InsightGenerationQueryServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID GENERATION_ID = UUID.randomUUID();

    @Mock
    private InsightRepository insightRepository;

    @Mock
    private Insight insight;

    private InsightGenerationQueryService service;

    @BeforeEach
    void setUp() {
        service = new InsightGenerationQueryService(
                insightRepository
        );
    }

    @Test
    void returnsOwnedGenerationStatus() {
        // given
        LocalDateTime requestedAt =
                LocalDateTime.of(2026, 8, 6, 16, 30);

        when(insightRepository.findByIdAndUser_Id(
                GENERATION_ID,
                USER_ID
        )).thenReturn(Optional.of(insight));
        when(insight.getId()).thenReturn(GENERATION_ID);
        when(insight.getStatus())
                .thenReturn(InsightGenerationStatus.FAILED);
        when(insight.getRequestedAt()).thenReturn(requestedAt);
        when(insight.getFailedAt())
                .thenReturn(requestedAt.plusSeconds(7));
        when(insight.getFailureCode()).thenReturn("I002");
        when(insight.getFailureMessage())
                .thenReturn("Insight recommendation service failed.");

        // when
        InsightGenerationStatusResponse result =
                service.getStatus(USER_ID, GENERATION_ID);

        // then
        assertThat(result.generationId())
                .isEqualTo(GENERATION_ID);
        assertThat(result.status())
                .isEqualTo(InsightGenerationStatus.FAILED);
        assertThat(result.requestedAt()).isEqualTo(requestedAt);
        assertThat(result.failedAt())
                .isEqualTo(requestedAt.plusSeconds(7));
        assertThat(result.failureCode()).isEqualTo("I002");
        assertThat(result.failureMessage())
                .isEqualTo("Insight recommendation service failed.");
    }

    @Test
    void hidesGenerationOwnedByAnotherUser() {
        // given
        when(insightRepository.findByIdAndUser_Id(
                GENERATION_ID,
                USER_ID
        )).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(
                () -> service.getStatus(USER_ID, GENERATION_ID)
        )
                .isInstanceOf(CustomException.class)
                .satisfies(exception ->
                        assertThat(((CustomException) exception)
                                .getErrorCode())
                                .isEqualTo(
                                        InsightErrorCode.INSIGHT_NOT_FOUND
                                )
                );
    }
}
