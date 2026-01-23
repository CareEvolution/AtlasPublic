package org.ohdsi.webapi.common.generation;

import org.ohdsi.webapi.Constants;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class CancelJobListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStepExecutions().stream()
                .anyMatch(se -> Objects.equals(Constants.CANCELED, se.getExitStatus().getExitCode()))) {
            jobExecution.setExitStatus(new ExitStatus(Constants.CANCELED, "Canceled by user request"));
        }
    }
}
