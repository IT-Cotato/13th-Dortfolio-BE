package com.itcotato.dortfolio.domain.record.analysis.embedding;

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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class StrengthTagEmbeddingService {

	private final StrengthTagRepository strengthTagRepository;
	private final StrengthTagEmbeddingRepository embeddingRepository;
	private final StrengthTagEmbeddingTextBuilder textBuilder;
	private final EmbeddingClient embeddingClient;
	private final StrengthTagEmbeddingWriter embeddingWriter;
	private final InsightProperties insightProperties;
	private final StrengthTagEmbeddingRequestProperties requestProperties;

	public boolean generate(UUID strengthTagId) {
		String targetModel = insightProperties.embeddingModel();
		if (embeddingRepository.existsByStrengthTag_IdAndEmbeddingModel(strengthTagId, targetModel)) {
			return false;
		}

		StrengthTag strengthTag = strengthTagRepository.findById(strengthTagId)
			.orElseThrow(() -> new CustomException(RecordAnalysisErrorCode.STRENGTH_TAG_NOT_FOUND));

		EmbeddingResponse response = requestEmbedding(strengthTagId, textBuilder.build(strengthTag));
		validate(response, targetModel);

		return embeddingWriter.saveIfAbsent(
			strengthTagId,
			response.embeddingModel(),
			response.embedding()
		);
	}

	public StrengthTagEmbeddingBatchResult generateAllMissing() {
		String targetModel = insightProperties.embeddingModel();
		long totalCount = strengthTagRepository.count();
		List<UUID> missingIds = strengthTagRepository.findMissingEmbeddingIdsByModel(targetModel);
		long generatedCount = 0;
		long skippedCount = totalCount - missingIds.size();
		List<StrengthTagEmbeddingFailure> failures = new ArrayList<>();

		for (int index = 0; index < missingIds.size(); index++) {
			UUID strengthTagId = missingIds.get(index);
			try {
				if (index > 0) {
					pause(requestProperties.interval());
				}
				if (generate(strengthTagId)) {
					generatedCount++;
				} else {
					skippedCount++;
				}
			} catch (Exception exception) {
				String reason = toFailureReason(exception);
				failures.add(new StrengthTagEmbeddingFailure(strengthTagId, reason));
				log.error(
					"Strength tag embedding generation failed. strengthTagId={}, reason={}",
					strengthTagId,
					reason,
					exception
				);
			}
		}

		return new StrengthTagEmbeddingBatchResult(
			totalCount,
			generatedCount,
			skippedCount,
			failures.size(),
			failures
		);
	}

	public StrengthTagEmbeddingCoverage inspectCoverage() {
		String targetModel = insightProperties.embeddingModel();
		return new StrengthTagEmbeddingCoverage(
			targetModel,
			strengthTagRepository.count(),
			embeddingRepository.countByEmbeddingModel(targetModel),
			strengthTagRepository.findMissingEmbeddingIdsByModel(targetModel)
		);
	}

	private EmbeddingResponse requestEmbedding(UUID strengthTagId, String sourceText) {
		EmbeddingRequest request = new EmbeddingRequest(sourceText);
		for (int attempt = 1; attempt <= requestProperties.maxAttempts(); attempt++) {
			try {
				return embeddingClient.embed(request);
			} catch (RestClientException exception) {
				if (attempt == requestProperties.maxAttempts()) {
					log.warn(
						"Strength tag embedding request failed after retries. strengthTagId={}, attempts={}",
						strengthTagId,
						attempt,
						exception
					);
					throw new CustomException(RecordAnalysisErrorCode.STRENGTH_TAG_EMBEDDING_AI_SERVICE_FAILED);
				}

				Duration backoff = requestProperties.initialBackoff()
					.multipliedBy(1L << Math.min(attempt - 1, 10));
				log.warn(
					"Strength tag embedding request failed; retrying. strengthTagId={}, attempt={}, maxAttempts={}, backoff={}",
					strengthTagId,
					attempt,
					requestProperties.maxAttempts(),
					backoff
				);
				pause(backoff);
			}
		}
		throw new IllegalStateException("Embedding retry loop completed unexpectedly");
	}

	private void validate(EmbeddingResponse response, String targetModel) {
		if (response == null
			|| !StringUtils.hasText(response.embeddingModel())
			|| !targetModel.equals(response.embeddingModel())
			|| response.embedding() == null
			|| response.embedding().length != StrengthTagEmbedding.EMBEDDING_DIMENSION) {
			throw new CustomException(RecordAnalysisErrorCode.STRENGTH_TAG_EMBEDDING_INVALID_RESPONSE);
		}

		boolean hasMagnitude = false;
		for (float value : response.embedding()) {
			if (!Float.isFinite(value)) {
				throw new CustomException(RecordAnalysisErrorCode.STRENGTH_TAG_EMBEDDING_INVALID_RESPONSE);
			}
			hasMagnitude |= value != 0.0f;
		}
		if (!hasMagnitude) {
			throw new CustomException(RecordAnalysisErrorCode.STRENGTH_TAG_EMBEDDING_INVALID_RESPONSE);
		}
	}

	private void pause(Duration duration) {
		if (duration.isZero()) {
			return;
		}
		try {
			Thread.sleep(duration.toMillis());
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new CustomException(RecordAnalysisErrorCode.STRENGTH_TAG_EMBEDDING_AI_SERVICE_FAILED);
		}
	}

	private String toFailureReason(Exception exception) {
		if (exception instanceof CustomException customException) {
			return customException.getErrorCode().getCode() + " " + customException.getErrorCode().getMessage();
		}
		return exception.getMessage() == null
			? exception.getClass().getSimpleName()
			: exception.getMessage();
	}
}
