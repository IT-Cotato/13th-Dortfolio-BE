package com.itcotato.dortfolio.domain.record.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.itcotato.dortfolio.domain.record.analysis.config.AiServiceProperties;
import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisRequest;
import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisRequest.ActivityPayload;
import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisRequest.AnswerPayload;
import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisRequest.StrengthTagCandidatePayload;
import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisRequest.TemplatePayload;
import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisResponse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FastApiRecordAnalysisClientTest {

	private HttpServer server;
	private AtomicReference<String> requestBody;

	@BeforeEach
	void setUp() throws IOException {
		requestBody = new AtomicReference<>();
		server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/ai/records/analyze", this::handleAnalyze);
		server.start();
	}

	@AfterEach
	void tearDown() {
		server.stop(0);
	}

	@Test
	@DisplayName("FastAPI 분석 요청에 JSON 본문을 포함해서 전송한다")
	void analyzeSendsJsonBody() {
		int port = server.getAddress().getPort();
		FastApiRecordAnalysisClient client = new FastApiRecordAnalysisClient(
			new AiServiceProperties("http://localhost:" + port, Duration.ofSeconds(1), Duration.ofSeconds(1))
		);

		UUID recordId = UUID.randomUUID();
		RecordAnalysisResponse response = client.analyze(new RecordAnalysisRequest(
			recordId,
			"기록 제목",
			new ActivityPayload("활동 제목", "활동 설명"),
			new TemplatePayload("템플릿"),
			List.of(new AnswerPayload("질문", "답변")),
			List.of(),
			List.of(new StrengthTagCandidatePayload(
				UUID.randomUUID(),
				"문제 해결",
				"문제를 정의하고 해결합니다.",
				"원인을 찾아 적절한 해결책을 실행합니다.",
				"병목을 찾아 응답 시간을 줄였습니다.",
				"문제를 다른 사람에게 넘기고 끝냈습니다.",
				0.82f
			)),
			2
		));

		assertThat(requestBody.get())
			.contains("\"recordId\":\"" + recordId + "\"")
			.contains("\"answers\"")
			.contains("\"strengthTagCandidates\"");
		assertThat(response.summary()).isEqualTo("요약");
		assertThat(response.evidenceSnippets()).containsExactly("근거");
		assertThat(response.strengthTagIds()).isEmpty();
	}

	private void handleAnalyze(HttpExchange exchange) throws IOException {
		requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
		byte[] response = """
			{
			  "summary": "요약",
			  "evidenceSnippets": ["근거"],
			  "strengthTagIds": []
			}
			""".getBytes(StandardCharsets.UTF_8);

		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(200, response.length);
		exchange.getResponseBody().write(response);
		exchange.close();
	}
}
