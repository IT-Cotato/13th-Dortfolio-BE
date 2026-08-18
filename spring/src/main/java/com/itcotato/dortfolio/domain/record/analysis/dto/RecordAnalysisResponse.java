package com.itcotato.dortfolio.domain.record.analysis.dto;

import java.util.List;
import java.util.UUID;

public record RecordAnalysisResponse(
	String summary,
	List<String> evidenceSnippets,
	List<UUID> strengthTagIds
) {
}
