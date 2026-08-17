package com.itcotato.dortfolio.domain.record.analysis.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itcotato.dortfolio.domain.insight.config.InsightProperties;
import com.itcotato.dortfolio.domain.record.analysis.exception.RecordAnalysisErrorCode;
import com.itcotato.dortfolio.domain.record.entity.StrengthTag;
import com.itcotato.dortfolio.domain.record.entity.StrengthTagEmbedding;
import com.itcotato.dortfolio.domain.record.repository.StrengthTagEmbeddingRepository;
import com.itcotato.dortfolio.domain.record.repository.StrengthTagRepository;
import com.itcotato.dortfolio.global.ai.embedding.dto.EmbeddingRequest;
import com.itcotato.dortfolio.global.ai.embedding.dto.EmbeddingResponse;
import com.itcotato.dortfolio.global.ai.embedding.service.EmbeddingClient;
import com.itcotato.dortfolio.global.exception.CustomException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StrengthTagEmbeddingServiceTest {

	private static final String MODEL = "gemini-embedding-2";

	@Mock
	private StrengthTagRepository strengthTagRepository;
	@Mock
	private StrengthTagEmbeddingRepository embeddingRepository;
	@Mock
	private EmbeddingClient embeddingClient;
	@Mock
	private StrengthTagEmbeddingWriter embeddingWriter;

	private StrengthTagEmbeddingService service;

	@BeforeEach
	void setUp() {
		InsightProperties insightProperties = new InsightProperties(
			10,
			Duration.ofHours(24),
			0.1,
			1,
			20,
			0.0,
			MODEL,
			2,
			Duration.ofSeconds(10)
		);
		service = new StrengthTagEmbeddingService(
			strengthTagRepository,
			embeddingRepository,
			new StrengthTagEmbeddingTextBuilder(),
			embeddingClient,
			embeddingWriter,
			insightProperties,
			new StrengthTagEmbeddingRequestProperties(Duration.ZERO, 1, Duration.ZERO)
		);
	}

	@Test
	void generatesEmbeddingFromStrengthDefinitionAndJudgementContext() {
		UUID strengthTagId = UUID.randomUUID();
		StrengthTag strengthTag = strengthTag();
		float[] vector = new float[StrengthTagEmbedding.EMBEDDING_DIMENSION];
		vector[0] = 1.0f;
		EmbeddingRequest expectedRequest = new EmbeddingRequest("""
			강점: 문제 해결
			정의: 문제를 구조적으로 분석하고 해결합니다.
			판단 기준: 원인을 파악하고 해결책을 실행했는지 확인합니다.
			적합 예시: 병목을 분석하고 개선했습니다.
			""".strip());
		when(embeddingRepository.existsByStrengthTag_IdAndEmbeddingModel(strengthTagId, MODEL))
			.thenReturn(false);
		when(strengthTagRepository.findById(strengthTagId)).thenReturn(Optional.of(strengthTag));
		when(embeddingClient.embed(expectedRequest)).thenReturn(new EmbeddingResponse(MODEL, vector));
		when(embeddingWriter.saveIfAbsent(strengthTagId, MODEL, vector)).thenReturn(true);

		boolean generated = service.generate(strengthTagId);

		assertThat(generated).isTrue();
		verify(embeddingWriter).saveIfAbsent(strengthTagId, MODEL, vector);
	}

	@Test
	void rejectsEmbeddingReturnedByDifferentModel() {
		UUID strengthTagId = UUID.randomUUID();
		when(embeddingRepository.existsByStrengthTag_IdAndEmbeddingModel(strengthTagId, MODEL))
			.thenReturn(false);
		when(strengthTagRepository.findById(strengthTagId)).thenReturn(Optional.of(strengthTag()));
		when(embeddingClient.embed(org.mockito.ArgumentMatchers.any()))
			.thenReturn(new EmbeddingResponse("another-model", new float[3072]));

		assertThatThrownBy(() -> service.generate(strengthTagId))
			.isInstanceOfSatisfying(CustomException.class, exception ->
				assertThat(exception.getErrorCode())
					.isEqualTo(RecordAnalysisErrorCode.STRENGTH_TAG_EMBEDDING_INVALID_RESPONSE)
			);
	}

	@Test
	void rejectsEmbeddingWithoutMagnitude() {
		UUID strengthTagId = UUID.randomUUID();
		when(embeddingRepository.existsByStrengthTag_IdAndEmbeddingModel(strengthTagId, MODEL))
			.thenReturn(false);
		when(strengthTagRepository.findById(strengthTagId)).thenReturn(Optional.of(strengthTag()));
		when(embeddingClient.embed(org.mockito.ArgumentMatchers.any()))
			.thenReturn(new EmbeddingResponse(MODEL, new float[StrengthTagEmbedding.EMBEDDING_DIMENSION]));

		assertThatThrownBy(() -> service.generate(strengthTagId))
			.isInstanceOfSatisfying(CustomException.class, exception ->
				assertThat(exception.getErrorCode())
					.isEqualTo(RecordAnalysisErrorCode.STRENGTH_TAG_EMBEDDING_INVALID_RESPONSE)
			);
	}

	private StrengthTag strengthTag() {
		return StrengthTag.create(
			"PROBLEM_SOLVING",
			"문제 해결",
			"문제를 구조적으로 분석하고 해결합니다.",
			"원인을 파악하고 해결책을 실행했는지 확인합니다.",
			"병목을 분석하고 개선했습니다.",
			"문제를 다른 사람에게 넘겼습니다."
		);
	}
}
