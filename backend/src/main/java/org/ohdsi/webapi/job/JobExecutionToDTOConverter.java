package org.ohdsi.webapi.job;

import org.ohdsi.webapi.Constants;
import org.ohdsi.webapi.exceptions.FatalException;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class JobExecutionToDTOConverter {
    public static JobExecutionResource convert(JobExecutionInfo entity) {

        final JobExecution execution = entity.getJobExecution();
        final JobInstance instance = execution.getJobInstance();
        final JobInstanceResource instanceResource = new JobInstanceResource(instance.getInstanceId(),
                instance.getJobName());
        var result = new JobExecutionResource(instanceResource, entity.getJobExecution().getId());

        final boolean isScriptExecution = execution.getJobParameters()
                .getString(Constants.JobSystem.SCRIPT_TYPE) != null;
        if (isScriptExecution) {
            throw new FatalException("Script execution not supported");
        } else {
            result.setStatus(execution.getStatus().name());
        }
        result.setExitStatus(execution.getExitStatus().getExitCode());
        result.setStartDate(Date.from(execution.getStartTime().atZone(ZoneId.systemDefault()).toInstant()));
        if (execution.getEndTime() != null) {
            result.setEndDate(Date.from(execution.getEndTime().atZone(ZoneId.systemDefault()).toInstant()));
        }
        result.setJobParametersResource(
                execution.getJobParameters().getParameters().entrySet()
                        .stream()
                        .filter(p -> Constants.ALLOWED_JOB_EXECUTION_PARAMETERS.contains(p.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> e.getValue().getValue() != null ? e.getValue().getValue() : "null")));
        result.setOwnerType(entity.getOwnerType());
        return result;
    }
}
