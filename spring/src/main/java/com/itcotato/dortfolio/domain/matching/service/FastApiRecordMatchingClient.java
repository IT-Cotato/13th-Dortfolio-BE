package com.itcotato.dortfolio.domain.matching.service;

import com.itcotato.dortfolio.domain.matching.dto.fastapi.QuestionEmbeddingRequest;
import com.itcotato.dortfolio.domain.matching.dto.fastapi.QuestionEmbeddingResponse;
import com.itcotato.dortfolio.domain.record.analysis.config.AiServiceProperties;
import com.itcotato.dortfolio.global.ai.observability.AiRequestHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Slf4j
@Component
public class FastApiRecordMatchingClient implements RecordMatchingClient {

	private final RestClient restClient;

	public FastApiRecordMatchingClient(AiServiceProperties aiServiceProperties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(aiServiceProperties.connectTimeout());
		requestFactory.setReadTimeout(aiServiceProperties.readTimeout());

		this.restClient = RestClient.builder()
			.baseUrl(aiServiceProperties.baseUrl().replaceAll("/$", ""))
			.requestFactory(requestFactory)
			.build();
	}

	@Override
	public QuestionEmbeddingResponse embedQuestion(String question) {
		return embedQuestion(UUID.randomUUID(), question);
	}

	@Override
	public QuestionEmbeddingResponse embedQuestion(
            UUID requestId,
            String question
    ) {
		log.debug("Record matching question embedding request started.");
		return restClient.post()
			    .uri("/ai/matching/question-embedding")
                .header(AiRequestHeaders.REQUEST_ID, requestId.toString())
                .contentType(MediaType.APPLICATION_JSON)
			    .accept(MediaType.APPLICATION_JSON)
			    .body(QuestionEmbeddingRequest.of(question))
			    .retrieve()
			    .body(QuestionEmbeddingResponse.class);
	}
}
