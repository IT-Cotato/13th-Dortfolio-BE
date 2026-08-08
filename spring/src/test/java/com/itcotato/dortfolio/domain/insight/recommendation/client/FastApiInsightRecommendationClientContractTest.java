package com.itcotato.dortfolio.domain.insight.recommendation.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itcotato.dortfolio.domain.insight.config.InsightProperties;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationCandidate;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationRequest;
import com.itcotato.dortfolio.domain.insight.recommendation.dto.RecommendationResult;
import com.itcotato.dortfolio.domain.record.analysis.config.AiServiceProperties;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class FastApiInsightRecommendationClientContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsAndReceivesFastApiContract() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID competencyId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        AtomicReference<JsonNode> receivedBody = new AtomicReference<>();
        AtomicReference<String> receivedMethod = new AtomicReference<>();
        AtomicReference<String> receivedContentType = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/ai/insights/recommendation", exchange -> {
            receivedMethod.set(exchange.getRequestMethod());
            receivedContentType.set(
                    exchange.getRequestHeaders().getFirst("Content-Type")
            );
            receivedBody.set(objectMapper.readTree(
                    exchange.getRequestBody()
            ));

            byte[] response = objectMapper.writeValueAsBytes(
                    java.util.Map.of(
                            "jobCompetencyId", competencyId,
                            "recordId", recordId,
                            "reason", "직무 역량을 잘 보여주는 기록입니다."
                    )
            );
            exchange.getResponseHeaders().set(
                    "Content-Type",
                    "application/json"
            );
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        FastApiInsightRecommendationClient client =
                new FastApiInsightRecommendationClient(
                        new AiServiceProperties(
                                "http://localhost:" + server.getAddress().getPort(),
                                Duration.ofSeconds(2),
                                Duration.ofSeconds(2)
                        ),
                        new InsightProperties(
                                10,
                                Duration.ofHours(24),
                                5,
                                "gemini-embedding-2",
                                2,
                                Duration.ofSeconds(2)
                        )
                );

        RecommendationRequest request = new RecommendationRequest(
                jobId,
                "백엔드 개발자",
                competencyId,
                "문제 해결",
                "복잡한 문제를 분석하고 해결하는 역량",
                List.of(new RecommendationCandidate(
                        recordId,
                        "API 성능 개선",
                        "문제 해결 경험",
                        "응답 시간을 개선했습니다.",
                        List.of("쿼리 실행 시간을 단축했습니다."),
                        0.91
                ))
        );

        RecommendationResult result = client.generate(request);

        assertThat(receivedMethod.get()).isEqualTo("POST");
        assertThat(receivedContentType.get()).startsWith("application/json");
        JsonNode body = receivedBody.get();
        assertThat(body.get("jobId").asText())
                .isEqualTo(jobId.toString());
        assertThat(body.get("jobCompetencyId").asText())
                .isEqualTo(competencyId.toString());
        assertThat(body.get("candidates").get(0).get("recordId").asText())
                .isEqualTo(recordId.toString());
        assertThat(body.get("candidates").get(0)
                .get("evidenceSnippets").get(0).asText())
                .isEqualTo("쿼리 실행 시간을 단축했습니다.");
        assertThat(result.jobCompetencyId()).isEqualTo(competencyId);
        assertThat(result.recordId()).isEqualTo(recordId);
        assertThat(result.reason())
                .isEqualTo("직무 역량을 잘 보여주는 기록입니다.");
    }
}
