package com.itcotato.dortfolio.domain.record.analysis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.itcotato.dortfolio.domain.record.analysis.dto.AnalyzedStrengthTagResponse;
import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisRequest;
import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisResponse;
import com.itcotato.dortfolio.domain.record.analysis.entity.RecordAnalysis;
import com.itcotato.dortfolio.domain.record.analysis.exception.RecordAnalysisErrorCode;
import com.itcotato.dortfolio.domain.record.analysis.repository.RecordAnalysisRepository;
import com.itcotato.dortfolio.domain.record.entity.Record;
import com.itcotato.dortfolio.domain.record.entity.RecordStrengthTag;
import com.itcotato.dortfolio.domain.record.entity.RecordStatus;
import com.itcotato.dortfolio.domain.record.entity.StrengthTag;
import com.itcotato.dortfolio.domain.record.repository.RecordEmbeddingRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordStrengthTagRepository;
import com.itcotato.dortfolio.domain.record.repository.StrengthTagRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
	private final RecordEmbeddingWriter recordEmbeddingWriter;
	private final TransactionTemplate transactionTemplate;
	private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

	public void analyze(UUID recordId) {
		log.info("Record AI analysis started. recordId={}", recordId);

		Optional<AnalysisSnapshot> snapshotOptional;
		try {
			snapshotOptional = transactionTemplate.execute(status -> prepareRequest(recordId));
		} catch (Exception exception) {
			recordAnalysisLockManager.executeWithLock(recordId, () -> markFailed(
				recordId,
				toFailureReason(new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_PERSISTENCE_FAILED)),
				RecordAnalysisErrorCode.RECORD_ANALYSIS_PERSISTENCE_FAILED.isRetryable()
			));
			log.warn("Record AI analysis prepare failed. recordId={}", recordId, exception);
			return;
		}
		if (snapshotOptional == null || snapshotOptional.isEmpty()) {
			log.info("Record AI analysis skipped. recordId={}", recordId);
			return;
		}

		RecordAnalysisResponse response;
		String evidenceSnippetsJson;
		try {
			RecordAnalysisRequest request = snapshotOptional.get().request();
			response = requestAnalysis(request);
			validate(response, request);
			evidenceSnippetsJson = toJson(response.evidenceSnippets());
		} catch (CustomException exception) {
			recordAnalysisLockManager.executeWithLock(
				recordId,
				() -> markFailed(recordId, toFailureReason(exception), toRetryable(exception))
			);
			log.warn(
				"Record AI analysis failed. recordId={}, errorCode={}, retryable={}",
				recordId,
				exception.getErrorCode().getCode(),
				toRetryable(exception)
			);
			return;
		}

		try {
			recordAnalysisLockManager.executeWithLock(recordId, () ->
				transactionTemplate.executeWithoutResult(status ->
					saveSuccessfulAnalysis(snapshotOptional.get(), response, evidenceSnippetsJson)
				)
			);
		} catch (Exception exception) {
			recordAnalysisLockManager.executeWithLock(recordId, () -> markFailed(
				recordId,
				toFailureReason(new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_PERSISTENCE_FAILED)),
				RecordAnalysisErrorCode.RECORD_ANALYSIS_PERSISTENCE_FAILED.isRetryable()
			));
			log.warn("Record AI analysis persistence failed. recordId={}", recordId, exception);
			return;
		}
		log.info("Record AI analysis completed. recordId={}", recordId);
	}

	private Optional<AnalysisSnapshot> prepareRequest(UUID recordId) {
		Optional<Record> recordOptional = recordRepository.findById(recordId);
		if (recordOptional.isEmpty() || !isAnalyzable(recordOptional.get())) {
			return Optional.empty();
		}
		Record record = recordOptional.get();

		return Optional.of(new AnalysisSnapshot(
			recordAnalysisRequestBuilder.build(record),
			record.getUpdatedAt()
		));
	}

	private void saveSuccessfulAnalysis(AnalysisSnapshot snapshot, RecordAnalysisResponse response, String evidenceSnippetsJson) {
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

		recordEmbeddingRepository.deleteAllByRecord_Id(recordId);
		recordStrengthTagRepository.deleteAllByRecord_Id(recordId);
		recordAnalysis.complete(response.summary(), evidenceSnippetsJson, snapshot.recordUpdatedAt());
		saveStrengthTags(record, response.strengthTags());
		recordEmbeddingWriter.save(record, response.embeddingModel(), response.embedding());
		recordAnalysisRepository.flush();
		recordStrengthTagRepository.flush();
	}

	private void markFailed(UUID recordId, String failureReason, boolean retryable) {
		transactionTemplate.executeWithoutResult(status ->
			recordRepository.findById(recordId)
				.filter(this::isAnalyzable)
				.ifPresent(record -> getOrCreate(record).fail(failureReason, retryable))
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

	private boolean isRetryableAiServiceStatus(HttpStatusCode statusCode) {
		return statusCode.is5xxServerError() || statusCode.value() == 429;
	}

	private void validate(RecordAnalysisResponse response, RecordAnalysisRequest request) {
		if (response == null) {
			throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_INVALID_RESPONSE);
		}
		if (!StringUtils.hasText(response.summary())) {
			throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_INVALID_RESPONSE);
		}
		if (response.evidenceSnippets() == null || response.evidenceSnippets().isEmpty()) {
			throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_INVALID_RESPONSE);
		}
		if (!StringUtils.hasText(response.embeddingModel())) {
			throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_INVALID_RESPONSE);
		}
		if (response.embedding() == null || response.embedding().length == 0) {
			throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_INVALID_RESPONSE);
		}
		validateStrengthTags(response.strengthTags(), request.strengthTagCandidates());
	}

	private void validateStrengthTags(
		List<AnalyzedStrengthTagResponse> strengthTags,
		List<RecordAnalysisRequest.StrengthTagCandidatePayload> candidates
	) {
		if (strengthTags == null) {
			throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_INVALID_RESPONSE);
		}
		Map<UUID, RecordAnalysisRequest.StrengthTagCandidatePayload> candidateMap = candidates.stream()
			.collect(Collectors.toMap(RecordAnalysisRequest.StrengthTagCandidatePayload::id, Function.identity()));
		Set<UUID> analyzedTagIds = strengthTags.stream()
			.map(AnalyzedStrengthTagResponse::strengthTagId)
			.collect(Collectors.toSet());
		if (analyzedTagIds.size() != strengthTags.size()) {
			throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_INVALID_RESPONSE);
		}

		for (AnalyzedStrengthTagResponse strengthTag : strengthTags) {
			if (!candidateMap.containsKey(strengthTag.strengthTagId())) {
				throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_INVALID_RESPONSE);
			}
			if (strengthTag.score() < 0.0f || strengthTag.score() > 1.0f) {
				throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_INVALID_RESPONSE);
			}
			if (Float.isNaN(strengthTag.score())) {
				throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_INVALID_RESPONSE);
			}
		}
	}

	private void saveStrengthTags(Record record, List<AnalyzedStrengthTagResponse> analyzedTags) {
		if (analyzedTags.isEmpty()) {
			return;
		}

		Map<UUID, StrengthTag> strengthTagMap = strengthTagRepository.findAllById(
				analyzedTags.stream()
					.map(AnalyzedStrengthTagResponse::strengthTagId)
					.toList()
			)
			.stream()
			.collect(Collectors.toMap(StrengthTag::getId, Function.identity()));

		if (strengthTagMap.size() != analyzedTags.stream()
			.map(AnalyzedStrengthTagResponse::strengthTagId)
			.collect(Collectors.toSet())
			.size()) {
			throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_INVALID_RESPONSE);
		}

		List<RecordStrengthTag> recordStrengthTags = analyzedTags.stream()
			.map(tag -> RecordStrengthTag.create(record, strengthTagMap.get(tag.strengthTagId()), tag.score()))
			.toList();

		recordStrengthTagRepository.saveAll(recordStrengthTags);
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
		return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
	}

	private boolean toRetryable(CustomException exception) {
		if (exception.getErrorCode() instanceof RecordAnalysisErrorCode recordAnalysisErrorCode) {
			return recordAnalysisErrorCode.isRetryable();
		}
		return false;
	}

	private record AnalysisSnapshot(
		RecordAnalysisRequest request,
		LocalDateTime recordUpdatedAt
	) {
	}
}
