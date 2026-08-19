package com.itcotato.dortfolio.domain.insight.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;

import com.itcotato.dortfolio.domain.insight.config.InsightProperties;
import com.itcotato.dortfolio.domain.insight.recommendation.client.InsightRecommendationClient;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationCandidate;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationRequest;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationResult;
import com.itcotato.dortfolio.domain.insight.recommendation.service.InsightRecommendationGeneratorImpl;
import com.itcotato.dortfolio.domain.insight.recommendation.service.InsightRecommendationRetrySleeper;
import com.itcotato.dortfolio.global.exception.CustomException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import com.itcotato.dortfolio.global.exception.types.InsightErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;

@ExtendWith(MockitoExtension.class)
class InsightRecommendationGeneratorImplTest {

    @Mock
    private InsightRecommendationClient client;

    @Mock
    private InsightRecommendationRetrySleeper retrySleeper;

    private InsightRecommendationGeneratorImpl generator;

    private UUID jobCompetencyId;
    private UUID candidateRecordId;
    private RecommendationRequest request;

    @BeforeEach
    void setUp() {
        generator = new InsightRecommendationGeneratorImpl(
                client,
                new InsightProperties(
                        10,
                        Duration.ofHours(24),
                        0.1,
                        1,
                        5,
                        0.0,
                        "gemini-embedding-2",
                        2,
                        Duration.ZERO,
                        Duration.ofSeconds(10)
                ),
                retrySleeper
        );

        jobCompetencyId = UUID.randomUUID();
        candidateRecordId = UUID.randomUUID();

        request = new RecommendationRequest(
                UUID.randomUUID(),
                "백엔드 개발자",
                jobCompetencyId,
                "문제 해결",
                "복잡한 문제를 분석하고 해결하는 역량",
                List.of(new RecommendationCandidate(
                        candidateRecordId,
                        "성능 문제 해결",
                        "문제 해결",
                        "API 성능 문제를 해결했습니다.",
                        List.of("쿼리 실행 시간을 단축했습니다."),
                        0.91
                ))
        );
    }

    @Test
    void generatesRecommendationFromCandidate() {
        when(client.generate(request))
                .thenReturn(new RecommendationResult(
                        jobCompetencyId,
                        candidateRecordId,
                        "성능 문제를 분석하고 해결한 경험입니다."
                ));

        RecommendationResult result =
                generator.generate(request);

        assertThat(result.recordId())
                .isEqualTo(candidateRecordId);
    }

    @Test
    void acceptsNoMatchResponse() {
        when(client.generate(request))
                .thenReturn(new RecommendationResult(
                        false,
                        jobCompetencyId,
                        null,
                        null
                ));

        RecommendationResult result = generator.generate(request);

        assertThat(result.matched()).isFalse();
        assertThat(result.recordId()).isNull();
    }

    @Test
    void rejectsNoMatchResponseContainingRecord() {
        when(client.generate(request))
                .thenReturn(new RecommendationResult(
                        false,
                        jobCompetencyId,
                        candidateRecordId,
                        "추천 이유"
                ));

        assertThatThrownBy(() -> generator.generate(request))
                .isInstanceOf(CustomException.class)
                .extracting(error ->
                        ((CustomException) error).getErrorCode()
                )
                .isEqualTo(
                        InsightErrorCode
                                .INSIGHT_RECOMMENDATION_INVALID_RESPONSE
                );
    }

    @Test
    void rejectsNoMatchResponseContainingEmptyReason() {
        when(client.generate(request))
                .thenReturn(new RecommendationResult(
                        false,
                        jobCompetencyId,
                        null,
                        ""
                ));

        assertThatThrownBy(() -> generator.generate(request))
                .isInstanceOf(CustomException.class)
                .extracting(error ->
                        ((CustomException) error).getErrorCode()
                )
                .isEqualTo(
                        InsightErrorCode
                                .INSIGHT_RECOMMENDATION_INVALID_RESPONSE
                );
    }

    @Test
    void rejectsNoMatchResponseContainingWhitespaceReason() {
        when(client.generate(request))
                .thenReturn(new RecommendationResult(
                        false,
                        jobCompetencyId,
                        null,
                        " "
                ));

        assertThatThrownBy(() -> generator.generate(request))
                .isInstanceOf(CustomException.class)
                .extracting(error ->
                        ((CustomException) error).getErrorCode()
                )
                .isEqualTo(
                        InsightErrorCode
                                .INSIGHT_RECOMMENDATION_INVALID_RESPONSE
                );
    }

    @Test
    void rejectsRecordOutsideCandidates() {
        when(client.generate(request))
                .thenReturn(new RecommendationResult(
                        jobCompetencyId,
                        UUID.randomUUID(),
                        "추천 이유"
                ));

        assertThatThrownBy(() -> generator.generate(request))
                .isInstanceOf(CustomException.class)
                .extracting(error ->
                        ((CustomException) error).getErrorCode()
                )
                .isEqualTo(
                        InsightErrorCode
                                .INSIGHT_RECOMMENDATION_INVALID_RESPONSE
                );
    }

    @Test
    void rejectsAnotherCompetencyId() {
        when(client.generate(request))
                .thenReturn(new RecommendationResult(
                        UUID.randomUUID(),
                        candidateRecordId,
                        "추천 이유"
                ));

        assertThatThrownBy(() -> generator.generate(request))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void rejectsBlankReason() {
        when(client.generate(request))
                .thenReturn(new RecommendationResult(
                        jobCompetencyId,
                        candidateRecordId,
                        " "
                ));

        assertThatThrownBy(() -> generator.generate(request))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void succeedsOnSecondAttempt() {
        when(client.generate(request))
                .thenThrow(new RestClientException("timeout"))
                .thenReturn(new RecommendationResult(
                        jobCompetencyId,
                        candidateRecordId,
                        "두 번째 시도에서 생성됐습니다."
                ));

        RecommendationResult result =
                generator.generate(request);

        assertThat(result.recordId())
                .isEqualTo(candidateRecordId);
        verify(client, times(2)).generate(request);
        verify(retrySleeper).sleep(Duration.ZERO);
    }

    @Test
    void retriesRateLimitResponse() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "30");
        when(client.generate(request))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Too Many Requests",
                        headers,
                        new byte[0],
                        null
                ))
                .thenReturn(new RecommendationResult(
                        jobCompetencyId,
                        candidateRecordId,
                        "재시도 후 생성됐습니다."
                ));

        RecommendationResult result = generator.generate(request);

        assertThat(result.recordId()).isEqualTo(candidateRecordId);
        verify(client, times(2)).generate(request);
        ArgumentCaptor<Duration> backoffCaptor =
                ArgumentCaptor.forClass(Duration.class);
        verify(retrySleeper).sleep(backoffCaptor.capture());
        assertThat(backoffCaptor.getValue())
                .isGreaterThanOrEqualTo(Duration.ofSeconds(30))
                .isLessThanOrEqualTo(Duration.ofSeconds(45));
    }

    @Test
    void retriesServerErrorResponse() {
        when(client.generate(request))
                .thenThrow(HttpServerErrorException.create(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Service Unavailable",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ))
                .thenReturn(new RecommendationResult(
                        jobCompetencyId,
                        candidateRecordId,
                        "재시도 후 생성됐습니다."
                ));

        RecommendationResult result = generator.generate(request);

        assertThat(result.recordId()).isEqualTo(candidateRecordId);
        verify(client, times(2)).generate(request);
    }

    @Test
    void doesNotRetryNonRetryableClientError() {
        when(client.generate(request))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.FORBIDDEN,
                        "Forbidden",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        assertThatThrownBy(() -> generator.generate(request))
                .isInstanceOf(CustomException.class)
                .extracting(error ->
                        ((CustomException) error).getErrorCode()
                )
                .isEqualTo(
                        InsightErrorCode
                                .INSIGHT_RECOMMENDATION_AI_SERVICE_FAILED
                );

        verify(client).generate(request);
        verify(retrySleeper, never()).sleep(any());
    }

    @Test
    void failsAfterMaximumAttempts() {
        when(client.generate(request))
                .thenThrow(new RestClientException("timeout"));

        assertThatThrownBy(() -> generator.generate(request))
                .isInstanceOf(CustomException.class)
                .extracting(error ->
                        ((CustomException) error).getErrorCode()
                )
                .isEqualTo(
                        InsightErrorCode
                                .INSIGHT_RECOMMENDATION_AI_SERVICE_FAILED
                );
        verify(client, times(2)).generate(request);
    }
}
