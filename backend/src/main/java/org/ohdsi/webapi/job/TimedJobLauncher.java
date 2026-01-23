package org.ohdsi.webapi.job;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.stereotype.Component;

import static org.ohdsi.webapi.Constants.Params.JOB_AUTHOR;
import static org.ohdsi.webapi.Constants.Params.JOB_START_TIME;
import static org.ohdsi.webapi.util.SecurityUtils.whitelist;

@Component
public class TimedJobLauncher {

    private static final Logger logger = LoggerFactory.getLogger(TimedJobLauncher.class);

    private final AuthorizationManager authorizationManager;
    private final JobLauncher jobLauncher;

    public TimedJobLauncher(
            AuthorizationManager authorizationManager, JobLauncher jobLauncher) {
        this.authorizationManager = authorizationManager;
        this.jobLauncher = jobLauncher;
    }

    public JobExecutionResource launch(final Job job, JobParameters jobParameters) throws WebApplicationException {
        JobExecution exec;
        try {
            JobParametersBuilder builder = new JobParametersBuilder(jobParameters);
            builder.addLong(JOB_START_TIME, System.currentTimeMillis());
            if (jobParameters.getString(JOB_AUTHOR) == null) {
                builder.addString(JOB_AUTHOR, authorizationManager.getCurrentUser().login());
            }
            jobParameters = builder.toJobParameters();
            exec = this.jobLauncher.run(job, jobParameters);
            logger.debug("JobExecution queued: {}", exec);
        } catch (final JobExecutionAlreadyRunningException e) {
            throw new WebApplicationException(e, Response.status(Status.CONFLICT).entity(whitelist(e)).build());
        } catch (final Exception e) {
            throw new WebApplicationException(e,
                    Response.status(Status.INTERNAL_SERVER_ERROR).entity(whitelist(e)).build());
        }
        return JobUtils.toJobExecutionResource(exec);
    }
}
