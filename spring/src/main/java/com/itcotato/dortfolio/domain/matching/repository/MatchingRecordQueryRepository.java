package com.itcotato.dortfolio.domain.matching.repository;

import com.itcotato.dortfolio.domain.matching.model.MatchingRecordCandidate;
import java.util.List;
import java.util.UUID;

public interface MatchingRecordQueryRepository {

	List<MatchingRecordCandidate> findVectorCandidates(
		UUID userId,
		String embeddingModel,
		float[] questionEmbedding,
		int limit
	);
}
