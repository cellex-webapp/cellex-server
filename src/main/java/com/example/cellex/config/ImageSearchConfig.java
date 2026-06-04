package com.example.cellex.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Cấu hình thread pool riêng cho Image Search indexing.
 * Tách biệt với main executor để không ảnh hưởng đến luồng xử lý chính.
 */
@Configuration
@EnableAsync
public class ImageSearchConfig {

    @Bean(name = "imageSearchExecutor")
    public Executor imageSearchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("img-search-");
        executor.setRejectedExecutionHandler(
                (r, e) -> {
                    // Log và bỏ qua nếu queue đầy (không block main thread)
                }
        );
        executor.initialize();
        return executor;
    }
}
