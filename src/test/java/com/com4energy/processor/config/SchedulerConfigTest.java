package com.com4energy.processor.config;

import com.com4energy.processor.common.SchedulerBeanNames;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.TaskScheduler;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulerConfigTest {

    @Test
    void shouldExposeExpectedSchedulerBeansAndAliases() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SchedulerConfig.class)) {
            assertTrue(context.containsBean(SchedulerBeanNames.PENDING_JOB_SCHEDULER_BEAN));
            assertTrue(context.containsBean(SchedulerBeanNames.RETRY_JOB_SCHEDULER_BEAN));
            assertTrue(context.containsBean(SchedulerBeanNames.DEFAULT_TASK_SCHEDULER_BEAN));

            TaskScheduler pendingScheduler =
                context.getBean(SchedulerBeanNames.PENDING_JOB_SCHEDULER_BEAN, TaskScheduler.class);
            TaskScheduler defaultScheduler =
                context.getBean(SchedulerBeanNames.DEFAULT_TASK_SCHEDULER_BEAN, TaskScheduler.class);
            TaskScheduler retryScheduler =
                context.getBean(SchedulerBeanNames.RETRY_JOB_SCHEDULER_BEAN, TaskScheduler.class);

            assertSame(pendingScheduler, defaultScheduler);
            assertNotSame(pendingScheduler, retryScheduler);
        }
    }
}

