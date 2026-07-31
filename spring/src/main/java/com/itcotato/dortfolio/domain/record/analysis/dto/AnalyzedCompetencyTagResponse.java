package com.itcotato.dortfolio.domain.record.analysis.dto;

import java.util.UUID;

public record AnalyzedCompetencyTagResponse(
	UUID competencyTagId,
	float score
) {
}
