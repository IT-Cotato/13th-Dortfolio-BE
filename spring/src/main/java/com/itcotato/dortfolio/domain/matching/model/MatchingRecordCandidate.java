package com.itcotato.dortfolio.domain.matching.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record MatchingRecordCandidate(
	UUID recordId,
	String recordTitle,
	LocalDateTime recordDate,
	UUID activityId,
	String activityType,
	String activityTitle,
	LocalDate startedAt,
	LocalDate endedAt,
	boolean isOngoing,
	String templateTitle,
	String summary,
	double candidateScore
) {
	public int matchRate() {
		return (int) Math.round(Math.max(0.0, Math.min(1.0, candidateScore)) * 100);
	}
}
