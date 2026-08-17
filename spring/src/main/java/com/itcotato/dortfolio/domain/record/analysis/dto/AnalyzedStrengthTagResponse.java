package com.itcotato.dortfolio.domain.record.analysis.dto;

import java.util.UUID;

public record AnalyzedStrengthTagResponse(
	UUID strengthTagId,
	float score
) {
}
