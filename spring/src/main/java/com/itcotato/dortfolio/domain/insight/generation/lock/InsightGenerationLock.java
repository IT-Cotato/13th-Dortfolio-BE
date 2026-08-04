package com.itcotato.dortfolio.domain.insight.generation.lock;

import com.itcotato.dortfolio.domain.insight.generation.config.InsightGenerationProperties;
import java.util.Collections;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/* Redis 락 */
@Component
@RequiredArgsConstructor
public class InsightGenerationLock {

    private static final String KEY_PREFIX =
            "insight:generation:lock:";

    // 락을 획득한 요청만 안전하게 락을 해제하도록 토큰 비교
    private static final DefaultRedisScript<Long>
            RELEASE_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            end
            return 0
            """,
            Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final InsightGenerationProperties properties;

    public String tryAcquire(UUID userId) {
        String key = key(userId);
        String token = UUID.randomUUID().toString();

        Boolean acquired = redisTemplate
                .opsForValue()
                .setIfAbsent(
                        key,
                        token,
                        properties.lockTtl()
                );

        return Boolean.TRUE.equals(acquired)
                ? token
                : null;
    }

    public void release(
            UUID userId,
            String token
    ) {
        if (token == null) {
            return;
        }

        redisTemplate.execute(
                RELEASE_SCRIPT,
                Collections.singletonList(key(userId)),
                token
        );
    }

    private String key(UUID userId) {
        return KEY_PREFIX + userId;
    }
}