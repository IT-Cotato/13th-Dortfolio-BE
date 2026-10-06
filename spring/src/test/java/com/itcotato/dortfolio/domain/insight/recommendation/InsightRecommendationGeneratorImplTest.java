package com.itcotato.dortfolio.domain.insight.recommendation;

import com.itcotato.dortfolio.domain.insight.config.InsightProperties;
import com.itcotato.dortfolio.domain.insight.recommendation.client.InsightRecommendationClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationCandidate;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationCompetency;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationRequest;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationResponse;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationResult;
import com.itcotato.dortfolio.domain.insight.recommendation.service.InsightRecommendationGeneratorImpl;
import com.itcotato.dortfolio.global.ai.generation.GenerationMetadata;
import com.itcotato.dortfolio.global.ai.retry.AiRetryExecutor;
import com.itcotato.dortfolio.global.ai.retry.AiRetryPolicy;
import com.itcotato.dortfolio.global.ai.retry.AiRetryProperties;
import com.itcotato.dortfolio.global.ai.retry.AiRetrySleeper;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.InsightErrorCode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

@ExtendWith(MockitoExtension.class)
class InsightRecommendationGeneratorImplTest {

    @Mock
    private InsightRecommendationClient client;
    @Mock
    private AiRetrySleeper retrySleeper;

    private InsightRecommendationGeneratorImpl generator;
    private RecommendationRequest request;
    private List<UUID> competencyIds;
    private List<UUID> recordIds;

    @BeforeEach
    void setUp() {
        generator = new InsightRecommendationGeneratorImpl(
                client,
                new InsightProperties(
                        10, Duration.ofHours(24), 0.1, 1, 5, 0.0,
                        "gemini-embedding-2", 2, Duration.ZERO,
                        Duration.ofSeconds(10)
                ),
                new AiRetryExecutor(new AiRetryPolicy(Clock.systemUTC()), new AiRetryProperties(3, Duration.ZERO),
                        retrySleeper, new SimpleMeterRegistry()),
                new AiRetryPolicy(Clock.systemUTC())
        );
        competencyIds = ids();
        recordIds = ids();
        request = new RecommendationRequest(
                UUID.randomUUID(),
                "백엔드 개발자",
                IntStream.range(0, 5)
                        .mapToObj(index -> new RecommendationCompetency(
                                competencyIds.get(index),
                                "역량 " + index,
                                "역량 설명 " + index,
                                List.of(candidate(recordIds.get(index)))
                        ))
                        .toList()
        );
    }

    @Test
    void generatesFiveRecommendationsInRequestOrder() {
        GenerationMetadata metadata = new GenerationMetadata(
                "insight_recommendation.v1", "insight_recommendation.v1", "test-generation", 8192, null
        );
        when(client.generate(request)).thenReturn(RecommendationResponse.of(
                IntStream.range(0, 5)
                        .map(index -> 4 - index)
                        .mapToObj(this::matched)
                        .toList()
        , metadata));

        RecommendationResponse result = generator.generate(request);
        assertThat(result.recommendations())
                .extracting(RecommendationResult::jobCompetencyId)
                .containsExactlyElementsOf(competencyIds);
        assertThat(result.metadata()).isSameAs(metadata);
        verify(client).generate(request);
    }

    @Test
    void acceptsNoMatchForCompetencyWithoutCandidates() {
        request = new RecommendationRequest(
                request.jobId(),
                request.jobName(),
                IntStream.range(0, 5)
                        .mapToObj(index -> new RecommendationCompetency(
                                competencyIds.get(index),
                                "역량 " + index,
                                "설명",
                                index == 0
                                        ? List.of()
                                        : List.of(candidate(recordIds.get(index)))
                        ))
                        .toList()
        );
        when(client.generate(request)).thenReturn(RecommendationResponse.of(
                IntStream.range(0, 5)
                        .mapToObj(index -> index == 0
                                ? new RecommendationResult(
                                        false, competencyIds.get(0), null, null
                                )
                                : matched(index))
                        .toList()
        , null));

        assertThat(generator.generate(request).recommendations().get(0).matched()).isFalse();
    }

    @Test
    void rejectsMissingCompetencyResultWithoutRetry() {
        when(client.generate(request)).thenReturn(RecommendationResponse.of(
                IntStream.range(0, 4).mapToObj(this::matched).toList()
        , null));

        assertInvalidResponse();
        verify(client).generate(request);
        verify(retrySleeper, never()).sleep(any());
    }

    @Test
    void rejectsDuplicateCompetencyResult() {
        when(client.generate(request)).thenReturn(RecommendationResponse.of(List.of(
                matched(0), matched(0), matched(1), matched(2), matched(3)
        ), null));

        assertInvalidResponse();
    }

    @Test
    void rejectsRecordOutsideItsCompetencyCandidates() {
        when(client.generate(request)).thenReturn(RecommendationResponse.of(
                IntStream.range(0, 5)
                        .mapToObj(index -> index == 0
                                ? new RecommendationResult(
                                        competencyIds.get(0),
                                        recordIds.get(1),
                                        "다른 역량 후보 기록"
                                )
                                : matched(index))
                        .toList()
        , null));

        assertInvalidResponse();
    }

    @Test
    void retriesTransientFailureAndReturnsBatch() {
        List<RecommendationResult> response =
                IntStream.range(0, 5).mapToObj(this::matched).toList();
        when(client.generate(request))
                .thenThrow(new ResourceAccessException("timeout"))
                .thenReturn(RecommendationResponse.of(response, null));

        assertThat(generator.generate(request).recommendations()).hasSize(5);
        verify(client, times(2)).generate(request);
        verify(retrySleeper).sleep(Duration.ZERO);
    }

    @Test
    void rejectsRequestThatDoesNotContainFiveCompetencies() {
        RecommendationRequest invalid = new RecommendationRequest(
                request.jobId(),
                request.jobName(),
                request.competencies().subList(0, 4)
        );

        assertThatThrownBy(() -> generator.generate(invalid))
                .isInstanceOf(CustomException.class);
        verify(client, never()).generate(any());
    }

    @Test
    void rejectsMalformedMetadataWithoutRetry() {
        when(client.generate(request)).thenReturn(RecommendationResponse.of(
                IntStream.range(0, 5).mapToObj(this::matched).toList(),
                new GenerationMetadata("", "v1", "model", 0, null)
        ));
        assertInvalidResponse();
        verify(retrySleeper, never()).sleep(any());
    }

    private List<UUID> ids() {
        return IntStream.range(0, 5)
                .mapToObj(index -> UUID.randomUUID())
                .toList();
    }

    private RecommendationCandidate candidate(UUID recordId) {
        return new RecommendationCandidate(
                recordId, "기록", "템플릿", "요약", List.of("근거"), 0.9
        );
    }

    private RecommendationResult matched(int index) {
        return new RecommendationResult(
                competencyIds.get(index),
                recordIds.get(index),
                "추천 이유 " + index
        );
    }

    private void assertInvalidResponse() {
        assertThatThrownBy(() -> generator.generate(request).recommendations())
                .isInstanceOf(CustomException.class)
                .extracting(error -> ((CustomException) error).getErrorCode())
                .isEqualTo(InsightErrorCode
                        .INSIGHT_RECOMMENDATION_INVALID_RESPONSE);
    }
}
