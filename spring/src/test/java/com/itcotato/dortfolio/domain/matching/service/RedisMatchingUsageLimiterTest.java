package com.itcotato.dortfolio.domain.matching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itcotato.dortfolio.domain.matching.config.MatchingProperties;
import com.itcotato.dortfolio.domain.matching.exception.MatchingErrorCode;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.util.RedisUtil;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RedisMatchingUsageLimiterTest {

	@Mock
	private RedisUtil redisUtil;

	private RedisMatchingUsageLimiter matchingUsageLimiter;

	@BeforeEach
	void setUp() {
		matchingUsageLimiter = new RedisMatchingUsageLimiter(
			redisUtil,
			new MatchingProperties(
				3,
				10,
				10,
				3072,
				java.util.List.of("test-embedding"),
				500,
				30,
				2,
				Duration.ofDays(1),
				ZoneId.of("Asia/Seoul"),
				java.util.List.of(new MatchingProperties.QuestionTag("TEST", "테스트 문항"))
			)
		);
	}

	@Test
	void validateDailyLimitAllowsUsageWithinLimit() {
		when(redisUtil.incrementWithExpireOnFirstUse(anyString(), org.mockito.ArgumentMatchers.anyLong())).thenReturn(1L);
		UUID userId = UUID.randomUUID();

		matchingUsageLimiter.validateDailyLimit(userId);

		ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
		verify(redisUtil).incrementWithExpireOnFirstUse(keyCaptor.capture(), org.mockito.ArgumentMatchers.anyLong());
		assertThat(keyCaptor.getValue())
			.isEqualTo("matching:daily:" + LocalDate.now(ZoneId.of("Asia/Seoul")) + ":" + userId);
	}

	@Test
	void validateDailyLimitRejectsExceededUsage() {
		when(redisUtil.incrementWithExpireOnFirstUse(anyString(), org.mockito.ArgumentMatchers.anyLong())).thenReturn(3L);

		assertThatThrownBy(() -> matchingUsageLimiter.validateDailyLimit(UUID.randomUUID()))
			.isInstanceOf(CustomException.class)
			.extracting(exception -> ((CustomException) exception).getErrorCode())
			.isEqualTo(MatchingErrorCode.MATCHING_DAILY_LIMIT_EXCEEDED);
	}
}
