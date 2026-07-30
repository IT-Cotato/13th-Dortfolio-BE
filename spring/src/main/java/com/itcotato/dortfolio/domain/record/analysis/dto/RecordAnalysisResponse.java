package com.itcotato.dortfolio.domain.record.analysis.dto;

import java.util.List;

public record RecordAnalysisResponse(
	String summary,
	List<String> evidenceSnippets,
	List<AnalyzedCompetencyTagResponse> competencyTags,
	String embeddingModel,
	float[] embedding
) {
}
