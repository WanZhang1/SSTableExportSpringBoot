package com.embrace.export.sstable.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author Alexis.Yang
 * @descriptioin
 * @date 2019/3/25 8:38 PM
 * @copyright www.embracesource.com
 */
@Configuration
public class TaskExecutorsConfig {
    @Value("${exportExecutor.core.size}")
    private int executorSize;

    @Value("${exportExecutor.queue.size}")
    private int queueSize;

    @Bean("exportExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(executorSize);
        executor.setMaxPoolSize(executorSize);
        executor.setQueueCapacity(queueSize);
        return executor;
    }
}
