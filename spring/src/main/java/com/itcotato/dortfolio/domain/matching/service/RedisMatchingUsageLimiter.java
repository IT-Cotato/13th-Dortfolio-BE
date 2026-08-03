package com.itcotato.dortfolio.domain.matching.service;

import com.itcotato.dortfolio.domain.matching.config.MatchingProperties;
import com.itcotato.dortfolio.domain.matching.exception.MatchingErrorCode;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.util.RedisUtil;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisMatchingUsageLimiter implements MatchingUsageLimiter {

	private static final String KEY_PREFIX = "matching:daily:";

	private final RedisUtil redisUtil;
	private final MatchingProperties matchingProperties;

	@Override
	public void validateDailyLimit(UUID userId) {
		String key = KEY_PREFIX + LocalDate.now() + ":" + userId;
		Long count = redisUtil.incrementWithExpireOnFirstUse(key, matchingProperties.dailyAiRequestTtl().toMillis());
		if (count != null && count > matchingProperties.dailyAiRequestLimit()) {
			throw new CustomException(MatchingErrorCode.MATCHING_DAILY_LIMIT_EXCEEDED);
		}
	}
}
