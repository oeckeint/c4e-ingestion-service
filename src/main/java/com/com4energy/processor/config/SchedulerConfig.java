package com.com4energy.processor.config;

import com.com4energy.processor.common.SchedulerBeanNames;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerConfig {

    private static final int SINGLE_THREAD_POOL_SIZE = 1;
    private static final String THREAD_PREFIX_PENDING = "sched-pending-";
    private static final String THREAD_PREFIX_RETRY = "sched-retry-";

    @Bean(name = {
        SchedulerBeanNames.PENDING_JOB_SCHEDULER_BEAN,
        SchedulerBeanNames.DEFAULT_TASK_SCHEDULER_BEAN
    })
    public TaskScheduler pendingJobScheduler() {
        return createScheduler(THREAD_PREFIX_PENDING);
    }

    @Bean(name = SchedulerBeanNames.RETRY_JOB_SCHEDULER_BEAN)
    public TaskScheduler retryJobScheduler() {
        return createScheduler(THREAD_PREFIX_RETRY);
    }

    private TaskScheduler createScheduler(String threadPrefix) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(SINGLE_THREAD_POOL_SIZE);
        scheduler.setThreadNamePrefix(threadPrefix);
        scheduler.initialize();
        return scheduler;
    }
}

