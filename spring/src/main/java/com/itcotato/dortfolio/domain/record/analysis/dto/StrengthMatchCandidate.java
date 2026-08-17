package com.itcotato.dortfolio.domain.record.analysis.dto;

import java.util.UUID;

public record StrengthMatchCandidate(
	UUID strengthTagId,
	String name,
	String description,
	String evaluationCriteria,
	String positiveExample,
	String negativeExample,
	float cosineSimilarity
) {
}
