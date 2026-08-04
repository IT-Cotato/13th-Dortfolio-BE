package com.itcotato.dortfolio.domain.insight.generation.config;

import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync
@Configuration
@RequiredArgsConstructor
public class InsightGenerationAsyncConfig {

    public static final String EXECUTOR_NAME =
            "insightGenerationExecutor";

    private final InsightGenerationProperties properties;

    @Bean(name = EXECUTOR_NAME)
    public Executor insightGenerationExecutor() {
        ThreadPoolTaskExecutor executor =
                new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(
                properties.asyncCorePoolSize()
        );
        executor.setMaxPoolSize(
                properties.asyncMaxPoolSize()
        );
        executor.setQueueCapacity(
                properties.asyncQueueCapacity()
        );
        // Insight 작업을 로그에서 구분할 수 있도록 전용 스레드 이름 사용
        executor.setThreadNamePrefix(
                "insight-generation-"
        );
        executor.initialize();

        return executor;
    }
}