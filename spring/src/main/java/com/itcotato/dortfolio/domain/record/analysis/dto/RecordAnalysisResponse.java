package com.itcotato.dortfolio.domain.record.analysis.dto;

import com.itcotato.dortfolio.global.ai.observability.dto.AiUsageResponse;

import java.util.List;
import java.util.UUID;

public record RecordAnalysisResponse(
	String summary,
	List<String> evidenceSnippets,
	List<UUID> strengthTagIds,
    AiUsageResponse usage
) {
	public RecordAnalysisResponse(String summary, List<String> evidenceSnippets, List<UUID> strengthTagIds) {
		this(summary, evidenceSnippets, strengthTagIds, null);
	}
}
