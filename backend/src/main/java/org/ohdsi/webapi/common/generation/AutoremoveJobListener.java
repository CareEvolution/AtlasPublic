package org.ohdsi.webapi.common.generation;

import org.ohdsi.webapi.job.JobService;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

public class AutoremoveJobListener implements JobExecutionListener {
    private final JobService jobService;

    public AutoremoveJobListener(JobService jobService) {
        this.jobService = jobService;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        // No action needed before the job starts
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        jobService.removeJob(jobExecution.getJobId());
    }
}
