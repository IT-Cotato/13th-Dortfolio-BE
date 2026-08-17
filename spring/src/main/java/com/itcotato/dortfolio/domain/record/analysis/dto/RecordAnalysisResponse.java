package com.itcotato.dortfolio.domain.record.analysis.dto;

import java.util.List;

public record RecordAnalysisResponse(
	String summary,
	List<String> evidenceSnippets,
	List<AnalyzedStrengthTagResponse> strengthTags,
	String embeddingModel,
	float[] embedding
) {
}
