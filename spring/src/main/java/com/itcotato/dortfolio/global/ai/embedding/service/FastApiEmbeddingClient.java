package com.itcotato.dortfolio.global.ai.embedding.service;

import com.itcotato.dortfolio.domain.record.analysis.config.AiServiceProperties;
import com.itcotato.dortfolio.global.ai.embedding.dto.EmbeddingRequest;
import com.itcotato.dortfolio.global.ai.embedding.dto.EmbeddingResponse;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class FastApiEmbeddingClient implements EmbeddingClient {

    private final RestClient restClient;

    public FastApiEmbeddingClient(
            AiServiceProperties properties
    ) {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(
                properties.connectTimeout()
        );
        requestFactory.setReadTimeout(
                properties.readTimeout()
        );

        this.restClient = RestClient.builder()
                .baseUrl(
                        properties.baseUrl()
                                .replaceAll("/$", "")
                )
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public EmbeddingResponse embed(
            EmbeddingRequest request
    ) {
        return restClient.post()
                .uri("/ai/matching/question-embedding")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(EmbeddingResponse.class);
    }
}
