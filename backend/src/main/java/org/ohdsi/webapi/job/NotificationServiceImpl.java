package org.ohdsi.webapi.job;

import org.ohdsi.webapi.Constants;
import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import static org.ohdsi.webapi.Constants.Params.SOURCE_KEY;

@Service
public class NotificationServiceImpl implements NotificationService {
    private static final int MAX_SIZE = 10;
    private static final List<String> WHITE_LIST = new ArrayList<>();
    private static final List<String> FOLDING_KEYS = new ArrayList<>();

    private final AuthorizationManager authorizationManager;
    private final JobService jobService;

    public NotificationServiceImpl(
        AuthorizationManager authorizationManager,
        JobService jobService, 
        List<GeneratesNotification> whiteList) {
        this.authorizationManager = authorizationManager;
        this.jobService = jobService;

        whiteList.forEach(g -> {
            WHITE_LIST.add(g.getJobName());
            FOLDING_KEYS.add(g.getExecutionFoldingKey());
        });
        // Folding key for warming source key job
        FOLDING_KEYS.add(SOURCE_KEY);
    }

    @Override
    public List<JobExecutionInfo> findLastJobs(List<BatchStatus> hideStatuses) {
        return findJobs(hideStatuses, MAX_SIZE, false);
    }

    @Override
    public List<JobExecutionInfo> findRefreshCacheLastJobs() {
        return findJobs(Collections.emptyList(), MAX_SIZE, true);
    }

    public List<JobExecutionInfo> findJobs(List<BatchStatus> hideStatuses, int maxSize, boolean refreshJobsOnly) {
        BiFunction<JobExecutionInfo, JobExecutionInfo, JobExecutionInfo> mergeFunction = (x, y) -> {
            final var xStartTime = x != null ? x.getJobExecution().getStartTime() : null;
            final var yStartTime = y != null ? y.getJobExecution().getStartTime() : null;
            return xStartTime != null ?
                    yStartTime != null ?
                            xStartTime.isAfter(yStartTime) ? x
                                    : y
                            : x
                    : y;
        };
        final Map<String, JobExecutionInfo> allJobMap = new HashMap<>();
        final Map<String, JobExecutionInfo> userJobMap = new HashMap<>();
        var executions = this.jobService.getJobExecutions();
        if (executions.isEmpty()) {
            return Collections.emptyList();
        }
        for (JobExecution jobExec : executions) {
            // ignore completed jobs when user does not want to see them
            if (hideStatuses.contains(jobExec.getStatus())) {
                continue;
            }
            if (!refreshJobsOnly && isInWhiteList(jobExec)) {
                var isMine = isMine(jobExec);
                if (userJobMap.size() < maxSize && isMine) {
                    var executionInfo = new JobExecutionInfo(jobExec, JobOwnerType.USER_JOB);
                    userJobMap.merge(getFoldingKey(jobExec), executionInfo, mergeFunction);
                }
                if (allJobMap.size() < maxSize) {
                    var executionInfo = new JobExecutionInfo(jobExec, JobOwnerType.ALL_JOB);
                    allJobMap.merge(getFoldingKey(jobExec), executionInfo, mergeFunction);
                }
            } else if (refreshJobsOnly) {
                if (allJobMap.size() < maxSize && jobExec.getJobInstance().getJobName().startsWith("warming ")) {
                    var executionInfo = new JobExecutionInfo(jobExec, JobOwnerType.ALL_JOB);
                    allJobMap.merge(getFoldingKey(jobExec), executionInfo, mergeFunction);
                }
            }

            if ((refreshJobsOnly || userJobMap.size() >= maxSize) && allJobMap.size() >= maxSize) {
                break;
            }
        }

        final List<JobExecutionInfo> jobs = new ArrayList<>(allJobMap.values());
        jobs.addAll(userJobMap.values());
        return jobs;
    }

    @Override
    public Date getLastViewedTime() {
        return authorizationManager.getCurrentUserLastViewedNotificationTime();
    }

    @Override
    public void setLastViewedTime(Date stamp) {
        authorizationManager.setCurrentUserLastViewedNotificationTime(stamp);
    }

    private static String getFoldingKey(JobExecution entity) {
        final Optional<String> key = entity.getJobParameters().getParameters().keySet().stream()
                .filter(FOLDING_KEYS::contains).findAny();
        return key.map(s -> s + "_" + entity.getJobParameters().getString(s) + "_" + entity.getJobParameters()
                        .getString("source_id"))
                .orElseGet(() -> String.valueOf(entity.getId()));
    }

    private static boolean isInWhiteList(JobExecution entity) {
        return WHITE_LIST.contains(entity.getJobInstance().getJobName());
    }

    private boolean isMine(JobExecution jobExec) {
        var userDTO = authorizationManager.getCurrentUser();
        var jobAuthor = jobExec.getJobParameters().getString(Constants.Params.JOB_AUTHOR);
        return userDTO.login().equals(jobAuthor);
    }
}
