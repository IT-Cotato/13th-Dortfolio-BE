package com.itcotato.dortfolio.domain.record.analysis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.itcotato.dortfolio.domain.record.analysis.dto.AnalyzedCompetencyTagResponse;
import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisRequest;
import com.itcotato.dortfolio.domain.record.analysis.dto.RecordAnalysisResponse;
import com.itcotato.dortfolio.domain.record.analysis.entity.RecordAnalysis;
import com.itcotato.dortfolio.domain.record.analysis.exception.RecordAnalysisErrorCode;
import com.itcotato.dortfolio.domain.record.analysis.repository.RecordAnalysisRepository;
import com.itcotato.dortfolio.domain.record.entity.CompetencyTag;
import com.itcotato.dortfolio.domain.record.entity.Record;
import com.itcotato.dortfolio.domain.record.entity.RecordCompetencyTag;
import com.itcotato.dortfolio.domain.record.entity.RecordStatus;
import com.itcotato.dortfolio.domain.record.repository.CompetencyTagRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordCompetencyTagRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordEmbeddingRepository;
import com.itcotato.dortfolio.domain.record.repository.RecordRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
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
	private final RecordCompetencyTagRepository recordCompetencyTagRepository;
	private final CompetencyTagRepository competencyTagRepository;
	private final RecordAnalysisLockManager recordAnalysisLockManager;
	private final RecordAnalysisRequestBuilder recordAnalysisRequestBuilder;
	private final RecordAnalysisClient recordAnalysisClient;
	private final RecordEmbeddingWriter recordEmbeddingWriter;
	private final TransactionTemplate transactionTemplate;
	private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

	public void analyze(UUID recordId) {
		recordAnalysisLockManager.executeWithLock(recordId, () -> analyzeLocked(recordId));
	}

	private void analyzeLocked(UUID recordId) {
		log.info("Record AI analysis started. recordId={}", recordId);

		Optional<RecordAnalysisRequest> requestOptional;
		try {
			requestOptional = transactionTemplate.execute(status -> prepareRequest(recordId));
		} catch (Exception exception) {
			markFailed(
				recordId,
				toFailureReason(new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_PERSISTENCE_FAILED)),
				RecordAnalysisErrorCode.RECORD_ANALYSIS_PERSISTENCE_FAILED.isRetryable()
			);
			log.warn("Record AI analysis prepare failed. recordId={}", recordId, exception);
			return;
		}
		if (requestOptional == null || requestOptional.isEmpty()) {
			log.info("Record AI analysis skipped. recordId={}", recordId);
			return;
		}

		RecordAnalysisResponse response;
		String evidenceSnippetsJson;
		try {
			RecordAnalysisRequest request = requestOptional.get();
			response = requestAnalysis(request);
			validate(response, request);
			evidenceSnippetsJson = toJson(response.evidenceSnippets());
		} catch (CustomException exception) {
			markFailed(recordId, toFailureReason(exception), toRetryable(exception));
			log.warn(
				"Record AI analysis failed. recordId={}, errorCode={}, retryable={}",
				recordId,
				exception.getErrorCode().getCode(),
				toRetryable(exception)
			);
			return;
		}

		try {
			transactionTemplate.executeWithoutResult(status -> saveSuccessfulAnalysis(recordId, response, evidenceSnippetsJson));
		} catch (Exception exception) {
			markFailed(
				recordId,
				toFailureReason(new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_PERSISTENCE_FAILED)),
				RecordAnalysisErrorCode.RECORD_ANALYSIS_PERSISTENCE_FAILED.isRetryable()
			);
			log.warn("Record AI analysis persistence failed. recordId={}", recordId, exception);
			return;
		}
		log.info("Record AI analysis completed. recordId={}", recordId);
	}

	private Optional<RecordAnalysisRequest> prepareRequest(UUID recordId) {
		Optional<Record> recordOptional = recordRepository.findById(recordId);
		if (recordOptional.isEmpty() || !isAnalyzable(recordOptional.get())) {
			return Optional.empty();
		}
		Record record = recordOptional.get();

		getOrCreatePending(record);
		clearDerivedAnalysisData(recordId);
		recordAnalysisRepository.flush();
		recordCompetencyTagRepository.flush();

		return Optional.of(recordAnalysisRequestBuilder.build(record));
	}

	private void saveSuccessfulAnalysis(UUID recordId, RecordAnalysisResponse response, String evidenceSnippetsJson) {
		Optional<Record> recordOptional = recordRepository.findById(recordId);
		if (recordOptional.isEmpty() || !isAnalyzable(recordOptional.get())) {
			clearDerivedAnalysisData(recordId);
			recordAnalysisRepository.deleteByRecord_Id(recordId);
			return;
		}
		Record record = recordOptional.get();
		RecordAnalysis recordAnalysis = getOrCreatePending(record);

		recordEmbeddingRepository.deleteAllByRecord_Id(recordId);
		recordCompetencyTagRepository.deleteAllByRecord_Id(recordId);
		recordAnalysis.complete(response.summary(), evidenceSnippetsJson);
		saveCompetencyTags(record, response.competencyTags());
		recordEmbeddingWriter.save(record, response.embeddingModel(), response.embedding());
		recordAnalysisRepository.flush();
		recordCompetencyTagRepository.flush();
	}

	private void clearDerivedAnalysisData(UUID recordId) {
		recordEmbeddingRepository.deleteAllByRecord_Id(recordId);
		recordCompetencyTagRepository.deleteAllByRecord_Id(recordId);
	}

	private void markFailed(UUID recordId, String failureReason, boolean retryable) {
		transactionTemplate.executeWithoutResult(status ->
			recordAnalysisRepository.findByRecord_Id(recordId)
				.ifPresent(recordAnalysis -> recordAnalysis.fail(failureReason, retryable))
		);
	}

	private RecordAnalysis getOrCreatePending(Record record) {
		return recordAnalysisRepository.findByRecord_Id(record.getId())
			.map(recordAnalysis -> {
				recordAnalysis.markPending();
				return recordAnalysis;
			})
			.orElseGet(() -> recordAnalysisRepository.save(RecordAnalysis.pending(record)));
	}

	private boolean isAnalyzable(Record record) {
		return record.getStatus() == RecordStatus.COMPLETED
			&& !record.isDeleted()
			&& !record.getActivity().isDeleted()
			&& !record.getTemplate().isDeleted();
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
		validateCompetencyTags(response.competencyTags(), request.competencyTagCandidates());
	}

	private void validateCompetencyTags(
		List<AnalyzedCompetencyTagResponse> competencyTags,
		List<RecordAnalysisRequest.CompetencyTagCandidatePayload> candidates
	) {
		if (competencyTags == null) {
			throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_INVALID_RESPONSE);
		}
		Map<UUID, RecordAnalysisRequest.CompetencyTagCandidatePayload> candidateMap = candidates.stream()
			.collect(Collectors.toMap(RecordAnalysisRequest.CompetencyTagCandidatePayload::id, Function.identity()));
		Set<UUID> analyzedTagIds = competencyTags.stream()
			.map(AnalyzedCompetencyTagResponse::competencyTagId)
			.collect(Collectors.toSet());
		if (analyzedTagIds.size() != competencyTags.size()) {
			throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_INVALID_RESPONSE);
		}

		for (AnalyzedCompetencyTagResponse competencyTag : competencyTags) {
			if (!candidateMap.containsKey(competencyTag.competencyTagId())) {
				throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_INVALID_RESPONSE);
			}
			if (competencyTag.score() < 0.0f || competencyTag.score() > 1.0f) {
				throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_INVALID_RESPONSE);
			}
		}
	}

	private void saveCompetencyTags(Record record, List<AnalyzedCompetencyTagResponse> analyzedTags) {
		if (analyzedTags.isEmpty()) {
			return;
		}

		Map<UUID, CompetencyTag> competencyTagMap = competencyTagRepository.findAllById(
				analyzedTags.stream()
					.map(AnalyzedCompetencyTagResponse::competencyTagId)
					.toList()
			)
			.stream()
			.collect(Collectors.toMap(CompetencyTag::getId, Function.identity()));

		if (competencyTagMap.size() != analyzedTags.stream()
			.map(AnalyzedCompetencyTagResponse::competencyTagId)
			.collect(Collectors.toSet())
			.size()) {
			throw new CustomException(RecordAnalysisErrorCode.RECORD_ANALYSIS_INVALID_RESPONSE);
		}

		List<RecordCompetencyTag> recordCompetencyTags = analyzedTags.stream()
			.map(tag -> RecordCompetencyTag.create(record, competencyTagMap.get(tag.competencyTagId()), tag.score()))
			.toList();

		recordCompetencyTagRepository.saveAll(recordCompetencyTags);
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
}
