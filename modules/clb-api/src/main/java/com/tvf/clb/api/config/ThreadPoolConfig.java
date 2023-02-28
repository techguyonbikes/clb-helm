package com.tvf.clb.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class ThreadPoolConfig {
    @Bean("crawlLadbrokesBet")
    public ThreadPoolTaskExecutor crawlMangaExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(50);
        executor.setMaxPoolSize(70);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("leafeon_crawl_manga_task_executor");
        executor.setRejectedExecutionHandler(new RejectedTaskHandler());
        executor.initialize();
        return executor;
    }
}
