package com.itcotato.dortfolio.domain.record.analysis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.itcotato.dortfolio.domain.record.analysis.config.RecordAnalysisProperties;
import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisRequest;
import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisResponse;
import com.itcotato.dortfolio.domain.record.analysis.dto.StrengthMatchCandidate;
import com.itcotato.dortfolio.domain.record.analysis.entity.RecordAnalysis;
import com.itcotato.dortfolio.domain.record.analysis.exception.RecordAnalysisErrorCode;
import com.itcotato.dortfolio.domain.record.analysis.repository.RecordAnalysisRepository;
import com.itcotato.dortfolio.domain.record.analysis.repository.StrengthMatchCandidateQuery;
import com.itcotato.dortfolio.domain.record.entity.Record;
import com.itcotato.dortfolio.domain.record.entity.RecordEmbedding;
import com.itcotato.dortfolio.domain.record.entity.RecordStatus;
import com.itcotato.dortfolio.domain.record.entity.RecordStrengthTag;
import com.itcotato.dortfolio.domain.record.entity.StrengthTag;
import com.itcotato.dortfolio.domain.record.repository.RecordEmbeddingRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordStrengthTagRepository;
import com.itcotato.dortfolio.domain.record.repository.StrengthTagRepository;
import com.itcotato.dortfolio.global.ai.embedding.dto.EmbeddingRequest;
import com.itcotato.dortfolio.global.ai.embedding.dto.EmbeddingResponse;
import com.itcotato.dortfolio.global.ai.embedding.service.EmbeddingClient;
import com.itcotato.dortfolio.global.exception.CustomException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordAnalysisService {

	private final RecordRepository recordRepository;
	private final RecordAnalysisRepository recordAnalysisRepository;
	private final RecordEmbeddingRepository recordEmbeddingRepository;
	private final RecordStrengthTagRepository recordStrengthTagRepository;
	private final StrengthTagRepository strengthTagRepository;
	private final RecordAnalysisLockManager recordAnalysisLockManager;
	private final RecordAnalysisRequestBuilder recordAnalysisRequestBuilder;
	private final RecordAnalysisClient recordAnalysisClient;
	private final EmbeddingClient embeddingClient;
	private final RecordEmbeddingTextBuilder recordEmbeddingTextBuilder;
	private final StrengthMatchCandidateQuery strengthMatchCandidateQuery;
	private final RecordEmbeddingWriter recordEmbeddingWriter;
	private final RecordAnalysisProperties recordAnalysisProperties;
	private final TransactionTemplate transactionTemplate;
	private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

	public void analyze(UUID recordId) {
		analyze(recordId, -1);
	}

	public void analyze(UUID recordId, long analysisGeneration) {
		log.info("Record AI analysis started. recordId={}", recordId);

		Optional<AnalysisSnapshot> snapshotOptional;
		try {
			snapshotOptional = transactionTemplate.execute(status -> prepareRequest(recordId, analysisGeneration));
		} catch (Exception exception) {
			markPersistenceFailure(recordId, "prepare", exception, null, -1);
			return;
		}
		if (snapshotOptional == null || snapshotOptional.isEmpty()) {
			log.info("Record AI analysis skipped. recordId={}", recordId);
			return;
		}

		AnalysisExecutionResult result;
		try {
			result = executeAnalysis(snapshotOptional.get().request());
		} catch (CustomException exception) {
			recordAnalysisLockManager.executeWithLock(
				recordId,
				() -> markFailed(recordId, toFailureReason(exception), toRetryable(exception),
					snapshotOptional.get().recordUpdatedAt(), snapshotOptional.get().analysisGeneration())
			);
			log.warn(
				"Record AI analysis failed. recordId={}, errorCode={}, retryable={}",
				recordId,
				exception.getErrorCode().getCode(),
				toRetryable(exception)
			);
			return;
		} catch (Exception exception) {
			markPersistenceFailure(recordId, "execute", exception,
				snapshotOptional.get().recordUpdatedAt(), snapshotOptional.get().analysisGeneration());
			return;
		}

		try {
			recordAnalysisLockManager.executeWithLock(recordId, () ->
				transactionTemplate.executeWithoutResult(status ->
					saveSuccessfulAnalysis(snapshotOptional.get(), result)
				)
			);
		} catch (CustomException exception) {
			recordAnalysisLockManager.executeWithLock(
				recordId,
				() -> markFailed(recordId, toFailureReason(exception), toRetryable(exception),
					snapshotOptional.get().recordUpdatedAt(), snapshotOptional.get().analysisGeneration())
			);
			log.warn(
				"Record AI analysis persistence rejected. recordId={}, errorCode={}",
				recordId,
				exception.getErrorCode().getCode()
			);
			return;
		} catch (Exception exception) {
			markPersistenceFailure(recordId, "persistence", exception,
				snapshotOptional.get().recordUpdatedAt(), snapshotOptional.get().analysisGeneration());
			return;
		}
		log.info("Record AI analysis completed. recordId={}", recordId);
	}

	private AnalysisExecutionResult executeAnalysis(RecordAnalysisRequest baseRequest) {
		EmbeddingResponse embedding = requestEmbedding(baseRequest);
		validateEmbedding(embedding);

		List<StrengthMatchCandidate> candidates = findStrengthCandidates(embedding);
		RecordAnalysisRequest analysisRequest = baseRequest.withStrengthCandidates(
			candidates,
			recordAnalysisProperties.strengthMaxCount()
		);
		RecordAnalysisResponse response = requestAnalysis(analysisRequest);
		validateAnalysisResponse(response, analysisRequest);

		return new AnalysisExecutionResult(
			response,
			embedding,
			candidates,
			toJson(response.evidenceSnippets())
		);
	}

	private Optional<AnalysisSnapshot> prepareRequest(UUID recordId, long analysisGeneration) {
		Optional<Record> recordOptional = recordRepository.findById(recordId);
		if (recordOptional.isEmpty() || !isAnalyzable(recordOptional.get())) {
			return Optional.empty();
		}
		Record record = recordOptional.get();
		if (analysisGeneration >= 0 && recordAnalysisRepository.findByRecord_Id(recordId)
			.map(analysis -> !analysis.isCurrentGeneration(analysisGeneration))
			.orElse(true)) {
			return Optional.empty();
		}
		return Optional.of(new AnalysisSnapshot(
			recordAnalysisRequestBuilder.build(record),
			record.getUpdatedAt(),
			analysisGeneration
		));
	}

	private EmbeddingResponse requestEmbedding(RecordAnalysisRequest request) {
		try {
			return embeddingClient.embed(new EmbeddingRequest(recordEmbeddingTextBuilder.build(request)));
		} catch (RestClientException exception) {
			log.warn("Record embedding request failed. recordId={}", request.recordId(), exception);
			throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_AI_SERVICE_FAILED);
		}
	}

	private List<StrengthMatchCandidate> findStrengthCandidates(EmbeddingResponse embedding) {
		try {
			return strengthMatchCandidateQuery.findTopCandidates(
				embedding.embeddingModel(),
				embedding.embedding(),
				recordAnalysisProperties.strengthCandidateLimit(),
				recordAnalysisProperties.strengthMinSimilarity()
			);
		} catch (CustomException exception) {
			throw exception;
		} catch (IllegalArgumentException exception) {
			log.warn("Strength match candidate arguments are invalid.", exception);
			throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_INVALID_RESPONSE);
		} catch (Exception exception) {
			log.warn("Strength match candidate query failed.", exception);
			throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_PERSISTENCE_FAILED);
		}
	}

	private RecordAnalysisResponse requestAnalysis(RecordAnalysisRequest request) {
		try {
			return recordAnalysisClient.analyze(request);
		} catch (RestClientResponseException exception) {
			log.warn(
				"Record AI service returned error. recordId={}, status={}, body={}",
				request.recordId(),
				exception.getStatusCode(),
				exception.getResponseBodyAsString()
			);
			if (isRetryableAiServiceStatus(exception.getStatusCode())) {
				throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_AI_SERVICE_FAILED);
			}
			throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_AI_SERVICE_REJECTED);
		} catch (Exception exception) {
			log.warn("Record AI service call failed. recordId={}", request.recordId(), exception);
			throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_AI_SERVICE_FAILED);
		}
	}

	private void validateEmbedding(EmbeddingResponse response) {
		if (response == null
			|| !StringUtils.hasText(response.embeddingModel())
			|| response.embedding() == null
			|| response.embedding().length != RecordEmbedding.EMBEDDING_DIMENSION) {
			throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_INVALID_RESPONSE);
		}
		boolean hasMagnitude = false;
		for (float value : response.embedding()) {
			if (!Float.isFinite(value)) {
				throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_INVALID_RESPONSE);
			}
			hasMagnitude |= value != 0.0f;
		}
		if (!hasMagnitude) {
			throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_INVALID_RESPONSE);
		}
	}

	private void validateAnalysisResponse(
		RecordAnalysisResponse response,
		RecordAnalysisRequest request
	) {
		if (response == null
			|| !StringUtils.hasText(response.summary())
			|| response.evidenceSnippets() == null
			|| response.evidenceSnippets().isEmpty()
			|| response.evidenceSnippets().size() > 5
			|| response.evidenceSnippets().stream()
				.anyMatch(snippet -> !StringUtils.hasText(snippet))
			|| response.strengthTagIds() == null) {
			throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_INVALID_RESPONSE);
		}

		Set<UUID> candidateIds = request.strengthTagCandidates().stream()
			.map(RecordAnalysisRequest.StrengthTagCandidatePayload::id)
			.collect(Collectors.toSet());
		if (response.strengthTagIds().stream().anyMatch(Objects::isNull)) {
			throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_INVALID_RESPONSE);
		}
		Set<UUID> selectedIds = new HashSet<>(response.strengthTagIds());
		if (selectedIds.size() != response.strengthTagIds().size()
			|| selectedIds.size() > request.maxStrengthCount()
			|| !candidateIds.containsAll(selectedIds)) {
			throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_INVALID_RESPONSE);
		}
	}

	private void saveSuccessfulAnalysis(
		AnalysisSnapshot snapshot,
		AnalysisExecutionResult result
	) {
		UUID recordId = snapshot.request().recordId();
		Optional<Record> recordOptional = recordRepository.findById(recordId);
		if (recordOptional.isEmpty() || !isAnalyzable(recordOptional.get())) {
			return;
		}
		Record record = recordOptional.get();
		if (!record.getUpdatedAt().equals(snapshot.recordUpdatedAt())) {
			log.info("Record AI analysis result skipped because record changed. recordId={}", recordId);
			return;
		}

		RecordAnalysis recordAnalysis = getOrCreate(record);
		if (snapshot.analysisGeneration() >= 0
			&& !recordAnalysis.isCurrentGeneration(snapshot.analysisGeneration())) {
			return;
		}
		recordEmbeddingRepository.deleteAllByRecord_Id(recordId);
		recordStrengthTagRepository.deleteAllByRecord_Id(recordId);
		recordAnalysis.complete(
			result.response().summary(),
			result.evidenceSnippetsJson(),
			snapshot.recordUpdatedAt()
		);
		saveStrengthTags(record, result.response().strengthTagIds(), result.candidates());
		recordEmbeddingWriter.save(
			record,
			result.embedding().embeddingModel(),
			result.embedding().embedding()
		);
		recordAnalysisRepository.flush();
		recordStrengthTagRepository.flush();
	}

	private void saveStrengthTags(
		Record record,
		List<UUID> selectedIds,
		List<StrengthMatchCandidate> candidates
	) {
		if (selectedIds.isEmpty()) {
			return;
		}

		Map<UUID, StrengthMatchCandidate> candidateMap = candidates.stream()
			.collect(Collectors.toMap(StrengthMatchCandidate::strengthTagId, Function.identity()));
		Map<UUID, StrengthTag> strengthTagMap = strengthTagRepository.findAllById(selectedIds)
			.stream()
			.collect(Collectors.toMap(StrengthTag::getId, Function.identity()));
		if (strengthTagMap.size() != selectedIds.size()) {
			throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_INVALID_RESPONSE);
		}

		List<RecordStrengthTag> recordStrengthTags = selectedIds.stream()
			.map(strengthTagId -> RecordStrengthTag.create(
				record,
				strengthTagMap.get(strengthTagId),
				candidateMap.get(strengthTagId).cosineSimilarity()
			))
			.toList();
		recordStrengthTagRepository.saveAll(recordStrengthTags);
	}

	private void markPersistenceFailure(
		UUID recordId,
		String phase,
		Exception exception,
		LocalDateTime expectedRecordUpdatedAt,
		long expectedGeneration
	) {
		CustomException persistenceException =
			new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_PERSISTENCE_FAILED);
		recordAnalysisLockManager.executeWithLock(recordId, () -> markFailed(
			recordId,
			toFailureReason(persistenceException),
			RecordAnalysisErrorCode.RECORD_ANALYSIS_PERSISTENCE_FAILED.isRetryable(),
			expectedRecordUpdatedAt,
			expectedGeneration
		));
		log.warn("Record AI analysis {} failed. recordId={}", phase, recordId, exception);
	}

	private void markFailed(
		UUID recordId,
		String failureReason,
		boolean retryable,
		LocalDateTime expectedRecordUpdatedAt,
		long expectedGeneration
	) {
		transactionTemplate.executeWithoutResult(status ->
			recordRepository.findById(recordId)
				.filter(this::isAnalyzable)
				.ifPresent(record -> {
					if (expectedRecordUpdatedAt != null
						&& !record.getUpdatedAt().equals(expectedRecordUpdatedAt)) {
						return;
					}
					RecordAnalysis analysis = getOrCreate(record);
					if (expectedGeneration >= 0 && !analysis.isCurrentGeneration(expectedGeneration)) {
						return;
					}
					analysis.fail(failureReason, retryable);
					recordEmbeddingRepository.deleteAllByRecord_Id(recordId);
					recordStrengthTagRepository.deleteAllByRecord_Id(recordId);
				})
		);
	}

	private RecordAnalysis getOrCreate(Record record) {
		return recordAnalysisRepository.findByRecord_Id(record.getId())
			.orElseGet(() -> recordAnalysisRepository.save(RecordAnalysis.pending(record)));
	}

	private boolean isAnalyzable(Record record) {
		return record.getStatus() == RecordStatus.COMPLETED
			&& !record.isDeleted()
			&& !record.getActivity().isDeleted();
	}

	private boolean isRetryableAiServiceStatus(HttpStatusCode statusCode) {
		return statusCode.is5xxServerError() || statusCode.value() == 429;
	}

	private String toJson(List<String> evidenceSnippets) {
		try {
			return objectMapper.writeValueAsString(evidenceSnippets);
		} catch (JsonProcessingException exception) {
			throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_EVIDENCE_SERIALIZE_FAILED);
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

	private boolean toRetryable(CustomException exception) {
		if (exception.getErrorCode() instanceof RecordAnalysisErrorCode recordAnalysisErrorCode) {
			return recordAnalysisErrorCode.isRetryable();
		}
		return false;
	}

	private record AnalysisSnapshot(
		RecordAnalysisRequest request,
		LocalDateTime recordUpdatedAt,
		long analysisGeneration
	) {
	}

	private record AnalysisExecutionResult(
		RecordAnalysisResponse response,
		EmbeddingResponse embedding,
		List<StrengthMatchCandidate> candidates,
		String evidenceSnippetsJson
	) {
	}
}
