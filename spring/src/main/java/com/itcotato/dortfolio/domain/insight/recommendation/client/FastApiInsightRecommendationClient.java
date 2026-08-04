package com.itcotato.dortfolio.domain.insight.recommendation.client;

import com.itcotato.dortfolio.domain.insight.config.InsightProperties;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationRequest;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationResult;
import com.itcotato.dortfolio.domain.record.analysis.config.AiServiceProperties;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class FastApiInsightRecommendationClient implements InsightRecommendationClient {

    private final RestClient restClient;

    public FastApiInsightRecommendationClient(
            AiServiceProperties aiServiceProperties,
            InsightProperties insightProperties
    ) {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(
                aiServiceProperties.connectTimeout()
        );

        requestFactory.setReadTimeout(
                insightProperties.recommendationTimeout()
        );

        this.restClient = RestClient.builder()
                .baseUrl(
                        aiServiceProperties
                                .baseUrl()
                                .replaceAll("/$", "")
                )
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public RecommendationResult generate(
            RecommendationRequest request
    ) {
        return restClient.post()
                .uri("/ai/insights/recommendation")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(RecommendationResult.class);
    }
}