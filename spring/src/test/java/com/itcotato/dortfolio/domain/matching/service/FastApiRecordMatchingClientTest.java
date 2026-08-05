package com.itcotato.dortfolio.domain.matching.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.itcotato.dortfolio.domain.matching.dto.fastapi.QuestionEmbeddingResponse;
import com.itcotato.dortfolio.domain.record.analysis.config.AiServiceProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FastApiRecordMatchingClientTest {

	private HttpServer server;
	private AtomicReference<String> embeddingRequestBody;

	@BeforeEach
	void setUp() throws IOException {
		embeddingRequestBody = new AtomicReference<>();
		server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/ai/matching/question-embedding", this::handleQuestionEmbedding);
		server.start();
	}

	@AfterEach
	void tearDown() {
		server.stop(0);
	}

	@Test
	void embedQuestionSendsQuestionAndParsesEmbedding() {
		FastApiRecordMatchingClient client = client();

		QuestionEmbeddingResponse response = client.embedQuestion("목표 달성 경험");

		assertThat(embeddingRequestBody.get()).contains("\"question\":\"목표 달성 경험\"");
		assertThat(response.embeddingModel()).isEqualTo("test-embedding");
		assertThat(response.embedding()).containsExactly(0.1f, 0.2f);
	}

	private FastApiRecordMatchingClient client() {
		int port = server.getAddress().getPort();
		return new FastApiRecordMatchingClient(
			new AiServiceProperties("http://localhost:" + port, Duration.ofSeconds(1), Duration.ofSeconds(1))
		);
	}

	private void handleQuestionEmbedding(HttpExchange exchange) throws IOException {
		embeddingRequestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
		byte[] response = """
			{
			  "embeddingModel": "test-embedding",
			  "embedding": [0.1, 0.2]
			}
			""".getBytes(StandardCharsets.UTF_8);
		writeResponse(exchange, response);
	}

	private void writeResponse(HttpExchange exchange, byte[] response) throws IOException {
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(200, response.length);
		exchange.getResponseBody().write(response);
		exchange.close();
	}
}
