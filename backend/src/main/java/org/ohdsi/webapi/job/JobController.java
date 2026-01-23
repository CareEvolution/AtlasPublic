package org.ohdsi.webapi.job;

import java.util.List;

import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;

/**
 * REST API for working with the Spring Batch jobs
 *
 */
@RestController
@RequestMapping("/job")
@RequestScope
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    /**
     * Get the job information by job ID
     *
     * @param jobId
     *         The job ID
     * @return The job information
     */
    @GetMapping("/{jobId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_JOB)")
    public JobInstanceResource getJob(@PathVariable final Long jobId) {
        JobInstanceResource job = this.jobService.getJob(jobId);
        return job;
    }

    /**
     * Get the job execution information by job type and name
     *
     * @param jobName
     *         The job name
     * @param jobType
     *         The job type
     * @return JobExecutionResource
     */
    @GetMapping("/type/{jobType}/name/{jobName}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_JOB)")
    public JobExecutionResource getJobByName(@PathVariable final String jobName,
            @PathVariable final String jobType) {
        return this.jobService.getJobExecutionByName(jobName, jobType);
    }

    /**
     * Get the job execution information by execution ID and job ID
     *
     * @param jobId
     *         The job ID
     * @param executionId
     *         The execution ID
     * @return JobExecutionResource
     */
    @GetMapping("/{jobId}/execution/{executionId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_JOB)")
    public JobExecutionResource findJobExecution(@PathVariable final Long jobId,
            @PathVariable final Long executionId) {
        return this.jobService.getJobExecution(jobId, executionId);
    }

    /**
     * Find job execution by execution ID
     *
     * @param executionId
     *         The job execution ID
     * @return JobExecutionResource
     */
    @GetMapping("/execution/{executionId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_JOB)")
    public JobExecutionResource findJobExecution(@PathVariable final Long executionId) {
        return this.jobService.getJobExecution(null, executionId);
    }

    /**
     * Get job names (unique names). Note: this path (GET /job) should really return pages of job instances. This could
     * be implemented should the need arise.
     *
     * @return A list of jobs
     */
    @GetMapping()
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_JOB)")
    public List<String> findJobNames() {
        return this.jobService.getJobNames();
    }

    /**
     * <i>Variation of spring-batch-admin support:
     * org.springframework.batch.admin.web.BatchJobExecutionsController</i>.
     * <p>
     * Return a paged collection of job executions. Filter for a given job. Returned in pages.
     *
     * @param jobName
     *         name of the job
     * @param pageIndex
     *         start index for the job execution list
     * @param pageSize
     *         page size for the list
     * @return collection of JobExecutionInfo
     * @throws NoSuchJobException
     */
    @GetMapping("/execution")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_JOB)")
    public Page<JobExecutionResource> getJobExecutionsPage(@RequestParam final String jobName,
            @RequestParam(defaultValue = "0") final Integer pageIndex,
            @RequestParam(defaultValue = "20") final Integer pageSize)
            throws NoSuchJobException {

        return this.jobService.getJobExecutionsPage(jobName, pageIndex, pageSize);
    }

    /**
     * Get all job executions
     *
     * @return A page of all job executions
     */
    @GetMapping("/allExecutions")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_JOB)")
    public List<JobExecutionResource> getAllJobExecutions() {
        return this.jobService.getAllJobExecutions();
    }
}
