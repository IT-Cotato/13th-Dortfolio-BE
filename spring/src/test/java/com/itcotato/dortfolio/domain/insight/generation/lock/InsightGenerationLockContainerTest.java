package com.itcotato.dortfolio.domain.insight.generation.lock;

import static org.assertj.core.api.Assertions.assertThat;

import com.itcotato.dortfolio.domain.insight.generation.config.InsightGenerationProperties;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class InsightGenerationLockContainerTest {

    @Container
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;

    @AfterEach
    void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void onlyOneConcurrentRequestAcquiresUserLock() throws Exception {
        connectionFactory = new LettuceConnectionFactory(
                REDIS.getHost(),
                REDIS.getMappedPort(6379)
        );
        connectionFactory.afterPropertiesSet();

        StringRedisTemplate redisTemplate =
                new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();

        InsightGenerationLock lock = new InsightGenerationLock(
                redisTemplate,
                new InsightGenerationProperties(
                        Duration.ofMinutes(5),
                        1,
                        1,
                        10
                )
        );

        UUID userId = UUID.randomUUID();
        int requestCount = 10;
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor =
                Executors.newFixedThreadPool(requestCount);

        try {
            List<Future<String>> attempts =
                    java.util.stream.IntStream.range(0, requestCount)
                            .mapToObj(index -> executor.submit(() -> {
                                ready.countDown();
                                start.await(5, TimeUnit.SECONDS);
                                return lock.tryAcquire(userId);
                            }))
                            .toList();

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<String> acquiredTokens = attempts.stream()
                    .map(this::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();

            assertThat(acquiredTokens).hasSize(1);

            lock.release(userId, acquiredTokens.get(0));
            assertThat(lock.tryAcquire(userId)).isNotNull();
        } finally {
            executor.shutdownNow();
        }
    }

    private String get(Future<String> future) {
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
