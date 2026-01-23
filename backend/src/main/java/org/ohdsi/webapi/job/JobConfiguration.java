package org.ohdsi.webapi.job;

import org.apache.commons.lang3.StringUtils;
import org.ohdsi.webapi.util.ManagedThreadPoolTaskExecutor;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;

@Configuration
public class JobConfiguration extends DefaultBatchConfiguration {

    @Value("${spring.batch.taskExecutor.corePoolSize}")
    private Integer corePoolSize;

    @Value("${spring.batch.taskExecutor.maxPoolSize}")
    private Integer maxPoolSize;

    @Value("${spring.batch.taskExecutor.queueCapacity}")
    private Integer queueCapacity;

    @Value("${spring.batch.taskExecutor.threadGroupName}")
    private String threadGroupName;

    @Value("${spring.batch.taskExecutor.threadNamePrefix}")
    private String threadNamePrefix;

    private final DataSource dataSource;

    public JobConfiguration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public DataSource getDataSource() {
        return this.dataSource;
    }

    @Override
    public TaskExecutor getTaskExecutor() {
        final ThreadPoolTaskExecutor taskExecutor = new ManagedThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(corePoolSize);
        taskExecutor.setMaxPoolSize(maxPoolSize);
        taskExecutor.setQueueCapacity(queueCapacity);
        if (StringUtils.isNotBlank(threadGroupName)) {
            taskExecutor.setThreadGroupName(threadGroupName);
        }
        if (StringUtils.isNotBlank(threadNamePrefix)) {
            taskExecutor.setThreadNamePrefix(threadNamePrefix);
        }
        taskExecutor.afterPropertiesSet();
        return taskExecutor;
    }
}