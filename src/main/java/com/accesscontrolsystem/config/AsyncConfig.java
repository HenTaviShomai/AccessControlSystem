package com.AccessControlSystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean("auditLogExecutor")
    public Executor auditLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数：即使空闲也保留的线程数
        executor.setCorePoolSize(2);
        
        // 最大线程数：当队列满了之后最多创建这么多线程
        executor.setMaxPoolSize(5);
        
        // 队列容量：能存放多少个任务排队
        executor.setQueueCapacity(100);
        
        // 线程名前缀
        executor.setThreadNamePrefix("audit-log-");
        
        // 拒绝策略：队列满了之后，由调用线程执行（保证日志不丢失）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.initialize();
        return executor;
    }
}
