package org.ohdsi.webapi.job;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import org.ohdsi.webapi.Constants;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;

public final class JobUtils {

    // Arrays.asList was used to provide easy extending ability in future
    private static List<String> PROTECTED_PARAMS = Arrays.asList(Constants.Params.UPDATE_PASSWORD);

    public static JobInstanceResource toJobInstanceResource(final JobInstance jobInstance) {
        final JobInstanceResource job = new JobInstanceResource(jobInstance.getId());
        job.setName(jobInstance.getJobName());
        return job;
    }

    public static JobExecutionResource toJobExecutionResource(final JobExecution jobExecution) {
        final JobExecutionResource execution = new JobExecutionResource(
                toJobInstanceResource(jobExecution.getJobInstance()), jobExecution.getId());
        execution.setStatus(jobExecution.getStatus().name());

        if (jobExecution.getStartTime() != null) {
            execution.setStartDate(Timestamp.valueOf(jobExecution.getStartTime()));
        }
        if (jobExecution.getEndTime() != null) {
            execution.setEndDate(Timestamp.valueOf(jobExecution.getEndTime()));
        }

        execution.setExitStatus(jobExecution.getExitStatus().getExitCode());
        var jobParams = jobExecution.getJobParameters();

        var params = jobParams.getParameters();
        if (params.isEmpty()) {
            return execution;
        }

        var jobParametersResource = new HashMap<String, Object>();
        Set<String> keys = params.keySet().stream()
                .filter(k -> !PROTECTED_PARAMS.contains(k))
                .collect(Collectors.toSet());
        for (var key : keys) {
            jobParametersResource.put(key, params.get(key).getValue());
        }
        execution.setJobParametersResource(jobParametersResource);

        return execution;
    }

    public static List<JobExecutionResource> toJobExecutionResource(final ResultSet rs) throws SQLException {
        List<JobExecutionResource> jobs = new ArrayList<>();
        JobExecutionResource jobexec = null;
        Map<String, Object> map = new HashMap<>();
        while (rs.next()) {
            Long id = rs.getLong(1);
            if (jobexec != null) {
                if (!jobexec.getExecutionId().equals(id)) {
                    jobexec.setJobParametersResource(map);
                    jobs.add(jobexec);
                    jobexec = null;
                }
            }
            if (jobexec == null) {
                map = new HashMap<String, Object>();
                JobInstance jobInstance = new JobInstance(rs.getLong(10), rs.getString(11));
                JobExecution jobExecution = new JobExecution(jobInstance, null);//jobParameters);
                jobExecution.setId(id);

                var startTime = rs.getTimestamp(2);
                if (startTime != null) {
                    jobExecution.setStartTime(startTime.toLocalDateTime());
                }
                var endTime = rs.getTimestamp(3);
                if (endTime != null) {
                    jobExecution.setEndTime(endTime.toLocalDateTime());
                }
                jobExecution.setStatus(BatchStatus.valueOf(rs.getString(4)));
                jobExecution.setExitStatus(new ExitStatus(rs.getString(5), rs.getString(6)));

                var createTime = rs.getTimestamp(7);
                if (createTime != null) {
                    jobExecution.setCreateTime(createTime.toLocalDateTime());
                }
                var lastUpdated = rs.getTimestamp(8);
                if (lastUpdated != null) {
                    jobExecution.setLastUpdated(lastUpdated.toLocalDateTime());
                }
                jobExecution.setVersion(rs.getInt(9));
                jobexec = toJobExecutionResource(jobExecution);
            }

            String key = rs.getString(12);

            if (!PROTECTED_PARAMS.contains(key)) {

                var parameterType = rs.getString(13);
                var parameterValue = rs.getString(14);
                var identifying = rs.getString(15).equalsIgnoreCase("Y");

                // These are the only types we appear to be using
                if (parameterType.toLowerCase().contains("string")) {
                    map.put(key, new JobParameter<>(parameterValue, String.class, identifying));
                } else if (parameterType.toLowerCase().contains("long")) {
                    Long value = parameterValue.isEmpty() ? 0 : Long.parseLong(parameterValue);
                    map.put(key, new JobParameter<>(value, Long.class, identifying));
                }
            }
        }

        if (jobexec != null && jobexec.getExecutionId() != null) {
            jobexec.setJobParametersResource(map);
            jobs.add(jobexec);
        }
        return jobs;
    }

}
