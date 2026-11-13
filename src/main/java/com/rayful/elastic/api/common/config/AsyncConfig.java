package com.rayful.elastic.api.common.config;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {
	
	@Value("${spring.task.execution.pool.core-size}")
    private int corePoolSize;

    @Value("${spring.task.execution.pool.max-size}")
    private int maxPoolSize;
    
    @Value("${spring.task.execution.pool.queue-capacity}")
    private int queryCapacity;
	
	@Bean(name = "asyncExecutor")
	public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize); // 코어 풀 크기 설정
        executor.setMaxPoolSize(maxPoolSize); // 최대 풀 크기 설정
        executor.setQueueCapacity(queryCapacity); // 큐 용량 설정
        executor.setThreadNamePrefix("AsyncThread-");
        executor.initialize();
        
        return executor;
    }
}
