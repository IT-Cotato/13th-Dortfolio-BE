package com.itcotato.dortfolio.domain.record.analysis.repository;

import com.itcotato.dortfolio.domain.record.analysis.dto.StrengthMatchCandidate;
import java.util.List;

public interface StrengthMatchCandidateQuery {

	List<StrengthMatchCandidate> findTopCandidates(
		String embeddingModel,
		float[] recordEmbedding,
		int limit,
		double minimumSimilarity
	);
}
