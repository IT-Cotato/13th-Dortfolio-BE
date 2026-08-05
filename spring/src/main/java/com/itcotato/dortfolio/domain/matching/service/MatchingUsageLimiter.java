package com.itcotato.dortfolio.domain.matching.service;

import java.util.UUID;

public interface MatchingUsageLimiter {

	void validateDailyLimit(UUID userId);
}
