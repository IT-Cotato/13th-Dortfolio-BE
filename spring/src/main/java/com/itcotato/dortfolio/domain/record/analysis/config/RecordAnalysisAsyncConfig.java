package com.itcotato.dortfolio.domain.record.analysis.config;

import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync
@Configuration
@RequiredArgsConstructor
public class RecordAnalysisAsyncConfig {

	private final RecordAnalysisProperties recordAnalysisProperties;

	@Bean(name = "recordAnalysisTaskExecutor")
	public Executor recordAnalysisTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(recordAnalysisProperties.asyncCorePoolSize());
		executor.setMaxPoolSize(recordAnalysisProperties.asyncMaxPoolSize());
		executor.setQueueCapacity(recordAnalysisProperties.asyncQueueCapacity());
		executor.setThreadNamePrefix("record-analysis-");
		executor.initialize();
		return executor;
	}
}
