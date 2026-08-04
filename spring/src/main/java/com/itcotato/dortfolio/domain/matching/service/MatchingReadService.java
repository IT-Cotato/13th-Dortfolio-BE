package com.itcotato.dortfolio.domain.matching.service;

import com.itcotato.dortfolio.domain.matching.config.MatchingProperties;
import com.itcotato.dortfolio.domain.matching.dto.res.MatchingRecordAnswerResponse;
import com.itcotato.dortfolio.domain.matching.dto.res.MatchingRecordResponse;
import com.itcotato.dortfolio.domain.matching.dto.res.MatchingSource;
import com.itcotato.dortfolio.domain.matching.model.MatchingRecordCandidate;
import com.itcotato.dortfolio.domain.matching.repository.MatchingRecordQueryRepository;
import com.itcotato.dortfolio.domain.record.entity.RecordAnswer;
import com.itcotato.dortfolio.domain.record.repository.RecordAnswerRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchingReadService {

	private final MatchingProperties matchingProperties;
	private final MatchingRecordQueryRepository matchingRecordQueryRepository;
	private final RecordAnswerRepository recordAnswerRepository;

	@Transactional(readOnly = true)
	public List<MatchingRecordResponse> findMatchingRecords(
		UUID userId,
		String embeddingModel,
		float[] questionEmbedding,
		int limit
	) {
		List<MatchingRecordCandidate> selectedRecords = matchingRecordQueryRepository.findVectorCandidates(
				userId,
				embeddingModel,
				questionEmbedding,
				matchingProperties.candidateRecordLimit()
			)
			.stream()
			.filter(candidate -> candidate.matchRate() >= matchingProperties.minMatchRate())
			.limit(limit)
			.toList();
		Map<UUID, List<MatchingRecordAnswerResponse>> answersByRecordId = findAnswersByRecordId(selectedRecords);
		return selectedRecords.stream()
			.map(candidate -> toRecordResponse(candidate, answersByRecordId.get(candidate.recordId())))
			.toList();
	}

	private Map<UUID, List<MatchingRecordAnswerResponse>> findAnswersByRecordId(
		List<MatchingRecordCandidate> candidates
	) {
		Map<UUID, List<MatchingRecordAnswerResponse>> answersByRecordId = new LinkedHashMap<>();
		candidates.forEach(candidate -> answersByRecordId.put(candidate.recordId(), new ArrayList<>()));

		List<UUID> recordIds = candidates.stream()
			.map(MatchingRecordCandidate::recordId)
			.toList();
		List<RecordAnswer> answers = recordIds.isEmpty()
			? List.of()
			: recordAnswerRepository.findAllByRecordIdsOrderByRecordIdAndSortOrder(recordIds);
		answers.forEach(answer -> answersByRecordId.computeIfAbsent(answer.getRecord().getId(), key -> new ArrayList<>())
			.add(MatchingRecordAnswerResponse.from(answer)));
		return answersByRecordId;
	}

	private MatchingRecordResponse toRecordResponse(
		MatchingRecordCandidate candidate,
		List<MatchingRecordAnswerResponse> answers
	) {
		return MatchingRecordResponse.of(
			candidate.recordId(),
			candidate.recordTitle(),
			candidate.activityId(),
			candidate.activityType(),
			candidate.activityTitle(),
			candidate.templateTitle(),
			candidate.recordDate(),
			candidate.matchRate(),
			MatchingSource.VECTOR,
			candidate.summary(),
			answers
		);
	}
}
