package org.ohdsi.webapi.job;

import org.ohdsi.webapi.Constants;
import org.ohdsi.webapi.audittrail.listeners.AuditTrailJobListener;
import org.ohdsi.webapi.common.generation.CancelJobListener;
import org.ohdsi.webapi.util.PreparedStatementRenderer;
import org.ohdsi.webapi.webapiproperties.WebApiProperties;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.core.step.StepLocator;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.StoppableTasklet;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.repeat.exception.ExceptionHandler;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.annotation.PostConstruct;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class JobService {
    private final JobExplorer jobExplorer;
    private final JobRepository jobRepository;
    private final TimedJobLauncher jobLauncher;
    private final PlatformTransactionManager transactionManager;
    private final TransactionTemplate transactionTemplateRequiresNew;
    private final AuditTrailJobListener auditTrailJobListener;
    private final CancelJobListener cancelJobListener;
    private final JdbcTemplate jdbcTemplate;
    private Map<Long, Job> jobMap = new HashMap<>();
    private WebApiProperties webApiProperties;

    private static final String INVALIDATED_BY_SYSTEM_EXIT_MESSAGE = "Invalidated by system";
    private static final int MAX_JOB_INSTANCES = 100;

    public JobService(JobExplorer jobExplorer, 
        JobRepository jobRepository,
        TimedJobLauncher timedJobLauncher, 
        TransactionTemplate transactionTemplateRequiresNew,
        PlatformTransactionManager transactionManager, 
        AuditTrailJobListener auditTrailJobListener,
        CancelJobListener cancelJobListener,
        JdbcTemplate jdbcTemplate,
        WebApiProperties webApiProperties) {
        this.jobExplorer = jobExplorer;
        this.jobRepository = jobRepository;
        this.jobLauncher = timedJobLauncher;
        this.transactionTemplateRequiresNew = transactionTemplateRequiresNew;
        this.transactionManager = transactionManager;
        this.auditTrailJobListener = auditTrailJobListener;
        this.cancelJobListener = cancelJobListener;
        this.jdbcTemplate = jdbcTemplate;
        this.webApiProperties = webApiProperties;
    }

    @PostConstruct
    private void invalidateJobsLeftInRunningState() {
        transactionTemplateRequiresNew.execute(s -> {
            List<String> jobNames = jobExplorer.getJobNames();
            List<JobExecution> jobExecutions = jobNames.stream()
            .flatMap(jobName -> jobExplorer.findRunningJobExecutions(jobName).stream())
            .collect(Collectors.toList());
            jobExecutions.forEach(this::invalidateJobExecution);
            return null;
        });
    }

    public void invalidateJobExecution(JobExecution job) {
        job.setStatus(BatchStatus.FAILED);
        job.setExitStatus(new ExitStatus(ExitStatus.FAILED.getExitCode(), INVALIDATED_BY_SYSTEM_EXIT_MESSAGE));
        job.setEndTime(LocalDateTime.ofInstant(Calendar.getInstance().toInstant(), ZoneId.systemDefault()));
        jobRepository.update(job);
    }

    public JobInstanceResource getJob(Long jobId) {
        final JobInstance job = this.jobExplorer.getJobInstance(jobId);
        if (job == null) {
            return null;
        }
        return JobUtils.toJobInstanceResource(job);
    }

    public JobExecutionResource getJobExecutionByName(String jobName, String jobType) {
        final Optional<JobExecution> jobExecution = jobExplorer.findRunningJobExecutions(jobType).stream()
                .filter(job -> jobName.equals(job.getJobParameters().getString(Constants.Params.JOB_NAME)))
                .findFirst();
        return jobExecution.map(JobUtils::toJobExecutionResource).orElse(null);
    }

    public JobExecution getJobExecution(Long jobExecutionId) {
        return jobExplorer.getJobExecution(jobExecutionId);
    }

    public JobExecutionResource getJobExecution(final Long jobId, final Long executionId) {
        final JobExecution exec = this.jobExplorer.getJobExecution(executionId);
        if ((exec == null) || ((jobId != null) && !jobId.equals(exec.getJobId()))) {
            return null;
        }
        return JobUtils.toJobExecutionResource(exec);
    }

    public List<String> getJobNames() {
        return this.jobExplorer.getJobNames();
    }

    public List<JobExecution> getJobExecutions() {
        List<JobExecution> executions = new ArrayList<>();
        List<String> jobNames = jobExplorer.getJobNames();
        for (String name : jobNames) {
            List<JobInstance> jobInstances = jobExplorer.findJobInstancesByJobName(name, 0, MAX_JOB_INSTANCES);
            for (JobInstance instance : jobInstances) {
                // TODO: This is loading all job parameters, execution context, and step contexts. I'm pretty 
                // sure we don't need all of that for the notifications screen (the only place this is used).
                // We should look into how to do a lightweight query that just gets the data we need.
                executions.addAll(jobExplorer.getJobExecutions(instance));
            }
        }
        return executions;
    }

    public List<JobExecutionResource> getAllJobExecutions() {
        String sqlPath = "/job/sql/jobExecutions.sql";
        String tqName = "ohdsi_schema";
        String tqValue = this.webApiProperties.appDb().schemaName();
        var psr = new PreparedStatementRenderer(null, sqlPath, tqName, tqValue);
        var resources = this.jdbcTemplate.query(psr.getSql(), psr.getSetter(),
                new ResultSetExtractor<List<JobExecutionResource>>() {
                    @Override
                    public List<JobExecutionResource> extractData(ResultSet rs)
                            throws SQLException, DataAccessException {

                        return JobUtils.toJobExecutionResource(rs);
                    }
                });
        return resources;
    }

    public Page<JobExecutionResource> getJobExecutionsPage(String jobName,
        Integer pageIndex,
        Integer pageSize) {

        List<JobExecutionResource> resources = new ArrayList<>();
        int offset = pageIndex * pageSize;

        if (jobName == null) {
            // Get all job names and fetch job instances and executions
            List<String> jobNames = jobExplorer.getJobNames();
            for (String name : jobNames) {
                List<JobInstance> jobInstances = jobExplorer.findJobInstancesByJobName(name, 0, Integer.MAX_VALUE);
                for (JobInstance instance : jobInstances) {
                    resources.addAll(fetchJobExecutionResources(instance));
                }
            }
        } else {
            // Fetch job instances and executions for the given job name
            List<JobInstance> jobInstances = jobExplorer.findJobInstancesByJobName(jobName, offset, pageSize);
            for (JobInstance instance : jobInstances) {
                resources.addAll(fetchJobExecutionResources(instance));
            }
        }

        // Create a paginated result
        int totalSize = resources.size();
        int endIndex = Math.min(offset + pageSize, totalSize);
        List<JobExecutionResource> paginatedResources = resources.subList(offset, endIndex);
        return new PageImpl<>(paginatedResources, PageRequest.of(pageIndex, pageSize), totalSize);
    }

    @Transactional
    public void cancelJobExecution(Predicate<? super JobExecution> filterPredicate) {
        jobExplorer.getJobNames().stream()
                .flatMap(jobName -> jobExplorer.findRunningJobExecutions(jobName).stream())
                .filter(filterPredicate)
                .findFirst()
                .ifPresent(jobExecution -> {
                    Job job = getRunningJob(jobExecution.getJobId());
                    if (Objects.nonNull(job)) {
                        stopJob(jobExecution, job);
                    }
                });
    }

    public void removeJob(Long jobExecutionId) {
        jobMap.remove(jobExecutionId);
    }

    public JobExecutionResource runJob(Job job, JobParameters jobParameters) {
        JobExecutionResource jobExecution = this.jobLauncher.launch(job, jobParameters);
        jobMap.put(jobExecution.getExecutionId(), job);
        return jobExecution;
    }

    public Step buildTaskletStep(String stepName, Tasklet tasklet) {
        return new StepBuilder(stepName, this.jobRepository)
                .tasklet(tasklet, this.transactionManager)
                .build();
    }

    public Step buildTaskletStep(String stepName, Tasklet tasklet, ExceptionHandler exceptionHandler) {
        return new StepBuilder(stepName, this.jobRepository)
                .tasklet(tasklet, this.transactionManager)
                .exceptionHandler(exceptionHandler)
                .build();
    }

    public Job buildJob(String jobName, List<Step> steps) {
        return buildJob(jobName, steps, Collections.emptyList());
    }

    public Job buildJob(String jobName, List<Step> steps, List<JobExecutionListener> listeners) {
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("Steps list cannot be null or empty");
        }

        JobBuilder jobBuilder = new JobBuilder(jobName, this.jobRepository);
        var result = jobBuilder.start(steps.get(0));
        for (int i = 1; i < steps.size(); i++) {
            result = result.next(steps.get(i));
        }
        result = result
            .listener(this.auditTrailJobListener)
            .listener(this.cancelJobListener);
        for (JobExecutionListener listener : listeners) {
            result = result.listener(listener);
        }
        return result.build();
    }

    /**
     * Fetches job execution resources for a given job instance.
     */
    private List<JobExecutionResource> fetchJobExecutionResources(JobInstance jobInstance) {
        List<JobExecutionResource> resources = new ArrayList<>();
        List<JobExecution> executions = jobExplorer.getJobExecutions(jobInstance);
        for (JobExecution execution : executions) {
            resources.add(JobUtils.toJobExecutionResource(execution));
        }
        return resources;
    }

    private void stopJob(JobExecution jobExecution, Job job) {
        if (Objects.nonNull(job)) {
            jobExecution.getStepExecutions().stream()
                    .filter(step -> step.getStatus().isRunning())
                    .forEach(stepExec -> {
                        Step step = ((StepLocator) job).getStep(stepExec.getStepName());
                        if (step instanceof TaskletStep taskletStep) {
                            Tasklet tasklet = taskletStep.getTasklet();
                            if (tasklet instanceof StoppableTasklet stoppableTasklet) {
                                StepSynchronizationManager.register(stepExec);
                                stoppableTasklet.stop();
                                StepSynchronizationManager.release();
                            }
                        }
                    });
        }
        if (jobExecution.getEndTime() == null) {
            jobExecution.setStatus(BatchStatus.STOPPING);
            jobRepository.update(jobExecution);
        }
    }

    private Job getRunningJob(Long jobExecutionId) {
        return jobMap.get(jobExecutionId);
    }
}
