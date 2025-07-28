package com.greendelta.bioheating.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig implements AsyncConfigurer {

	private final int poolSize;
	private final int maxPoolSize;
	private final int queueCapacity;

	public AsyncConfig(
		@Value("${bioheating.tasks.pool-size:4}") int poolSize,
		@Value("${bioheating.tasks.max-pool-size:8}") int maxPoolSize,
		@Value("${bioheating.tasks.queue-capacity:100}") int queueCapacity) {
		this.poolSize = poolSize;
		this.maxPoolSize = maxPoolSize;
		this.queueCapacity = queueCapacity;
	}

	@Override
	public Executor getAsyncExecutor() {
		var exec = new ThreadPoolTaskExecutor();
		exec.setCorePoolSize(poolSize);
		exec.setMaxPoolSize(maxPoolSize);
		exec.setQueueCapacity(queueCapacity);
		exec.setThreadNamePrefix("TaskExec-");
		exec.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		exec.initialize();
		return exec;
	}
}
