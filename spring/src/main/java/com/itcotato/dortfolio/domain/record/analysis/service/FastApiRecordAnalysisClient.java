package com.itcotato.dortfolio.domain.record.analysis.service;

import com.itcotato.dortfolio.domain.record.analysis.config.AiServiceProperties;
import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisRequest;
import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisResponse;
import com.itcotato.dortfolio.global.ai.observability.AiRequestHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Slf4j
@Component
public class FastApiRecordAnalysisClient implements RecordAnalysisClient {

	private final RestClient restClient;

	public FastApiRecordAnalysisClient(AiServiceProperties aiServiceProperties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(aiServiceProperties.connectTimeout());
		requestFactory.setReadTimeout(aiServiceProperties.readTimeout());

		this.restClient = RestClient.builder()
			.baseUrl(aiServiceProperties.baseUrl().replaceAll("/$", ""))
			.requestFactory(requestFactory)
			.requestInterceptor((request, body, execution) -> {
				log.debug(
					"Record AI request prepared. method={}, uri={}, contentType={}, bodyBytes={}",
					request.getMethod(),
					request.getURI(),
					request.getHeaders().getContentType(),
					body.length
				);
				return execution.execute(request, body);
			})
			.build();
	}

	@Override
	public RecordAnalysisResponse analyze(RecordAnalysisRequest request) {
		return analyze(UUID.randomUUID(), request);
	}

	@Override
	public RecordAnalysisResponse analyze(
            UUID requestId,
            RecordAnalysisRequest request
    ) {
		log.debug("Record AI request started. recordId={}", request.recordId());
		return restClient.post()
			    .uri("/ai/records/analyze")
                .header(AiRequestHeaders.REQUEST_ID, requestId.toString())
			    .contentType(MediaType.APPLICATION_JSON)
			    .accept(MediaType.APPLICATION_JSON)
			    .body(request)
			    .retrieve()
			    .body(RecordAnalysisResponse.class);
	}
}
