package org.ohdsi.webapi.cdmresults.action;

import org.apache.commons.lang3.StringUtils;
import org.ohdsi.webapi.Constants;
import org.ohdsi.webapi.achilles.service.AchillesCacheService;
import org.ohdsi.webapi.cdmresults.AchillesCacheTaskletFactory;
import org.ohdsi.webapi.cdmresults.CdmResultsCacheTaskletFactory;
import org.ohdsi.webapi.cdmresults.service.CDMCacheService;
import org.ohdsi.webapi.job.JobExecutionResource;
import org.ohdsi.webapi.job.JobService;
import org.ohdsi.webapi.source.SourceDaimonEntity;
import org.ohdsi.webapi.source.SourceEntity;
import org.ohdsi.webapi.source.SourceService;
import org.ohdsi.webapi.util.SourceUtils;
import org.ohdsi.webapi.webapiproperties.WebApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class CacheActions {
    private final Logger logger = LoggerFactory.getLogger(CacheActions.class);

    private final SourceService sourceService;
    private final JobService jobService;
    private final AchillesCacheService achillesCacheService;
    private final CDMCacheService cdmCacheService;
    private final WebApiProperties webApiProperties;
    private final AchillesCacheTaskletFactory achillesCacheTaskletFactory;
    private final CdmResultsCacheTaskletFactory cdmResultsCacheTaskletFactory;

    public CacheActions(
            SourceService sourceService,
            JobService jobService,
            AchillesCacheService achillesCacheService,
            CDMCacheService cdmCacheService,
            WebApiProperties webApiProperties,
            AchillesCacheTaskletFactory achillesCacheTaskletFactory,
            CdmResultsCacheTaskletFactory cdmResultsCacheTaskletFactory) {
        this.sourceService = sourceService;
        this.jobService = jobService;
        this.achillesCacheService = achillesCacheService;
        this.cdmCacheService = cdmCacheService;
        this.webApiProperties = webApiProperties;
        this.achillesCacheTaskletFactory = achillesCacheTaskletFactory;
        this.cdmResultsCacheTaskletFactory = cdmResultsCacheTaskletFactory;
    }

    @Transactional
    public void clearAllCaches() {
        achillesCacheService.clearCache();
        cdmCacheService.clearCache();
    }

    @Transactional
    public void clearCacheForSource(String sourceKey) {
        var source = sourceService.findBySourceKey(sourceKey);
        achillesCacheService.clearCache(source);
        cdmCacheService.clearCache(source);
    }

    public JobExecutionResource refreshCache(String sourceKey) {
        var source = sourceService.findBySourceKey(sourceKey);
        if (jobService.getJobExecutionByName(getWarmCacheJobName(String.valueOf(source.getSourceId()), sourceKey),
                getWarmCacheJobName(String.valueOf(source.getSourceId()), sourceKey)) == null) {
            return warmCaches(source);
        } else {
            return new JobExecutionResource();
        }
    }

    public List<SourceEntity> getAllSources() {
        return sourceService.getSources().stream().collect(Collectors.toList());
    }

    public SourceEntity getSource(String sourceKey) {
        return sourceService.findBySourceKey(sourceKey);
    }

    private List<Step> createCacheWarmingJobSteps(SourceEntity source, String jobName) {
        int resultDaimonPriority = getResultsDaimonPriority(source);
        List<Step> steps = new ArrayList<>();

        if (webApiProperties.cdmResults().achilles().enableCacheWarming()) {
            steps.add(getAchillesStep(source, jobName));
        }
        if (resultDaimonPriority > 0) {
            steps.add(getCountStep(source, jobName));
        }
        return steps;
    }

    private Job createJob(String jobName, List<Step> steps) {
        if (jobService.getJobExecutionByName(jobName, jobName) == null && !steps.isEmpty()) {
            return this.jobService.buildJob(jobName, steps);
        }
        return null;
    }

    private Step getAchillesStep(SourceEntity source, String jobStepName) {
       var achillesTasklet = achillesCacheTaskletFactory.create(source);
       return this.jobService.buildTaskletStep(jobStepName + " achilles", achillesTasklet);
    }

    private Step getCountStep(SourceEntity source, String jobStepName) {
        var countTasklet = cdmResultsCacheTaskletFactory.create(source);
        return this.jobService.buildTaskletStep(jobStepName + " counts", countTasklet);
    }

    // TODO there are some hard-coded numbers (100, 88,etc) here
    // TODO see comment "this is quick fix" ... what should be done?
    private String getJobName(String jobType, String sourceIds, String sourceKeys) {
        // for multiple sources: try to compose a job name from source keys, and if it
        // is too long - use source ids
        var jobName = String.format("%s: %s", jobType, sourceKeys);

        if (jobName.length() >= 100) { // job name in batch_job_instance is varchar(100)
            jobName = String.format("%s: %s", jobType, sourceIds);

            if (jobName.length() >= 100) { // if we still have more than 100 symbols
                jobName = jobName.substring(0, 88);
                jobName = jobName.substring(0, jobName.lastIndexOf(','))
                        .concat(" and more..."); // todo: this is quick fix. need better solution
            }
        }
        return jobName;
    }

    private int getResultsDaimonPriority(SourceEntity source) {
        Optional<Integer> resultsPriority = source.getDaimons().stream()
                .filter(d -> d.getDaimonType().equals(SourceDaimonEntity.DaimonType.Results))
                .map(SourceDaimonEntity::getPriority)
                .filter(p -> p > 0)
                .findAny();
        return resultsPriority.orElse(0);
    }

    private String getWarmCacheJobName(String sourceIds, String sourceKeys) {
        return getJobName("warming cache", sourceIds, sourceKeys);
    }

    // TODO fix hardcoding
    private JobExecutionResource runJob(String sourceKey, int sourceId, String jobName, Job job) {
        return jobService.runJob(job, new JobParametersBuilder()
                .addString(Constants.Params.JOB_NAME, jobName)

                // batch_job_execution_params.string_val is varchar(250). too many source keys
                // can exceed 250 symbols
                .addString(Constants.Params.SOURCE_KEY, StringUtils.substring(sourceKey, 0, 250))

                .addString(Constants.Params.SOURCE_ID, String.valueOf(sourceId))
                .toJobParameters());
    }

    private JobExecutionResource warmCaches(SourceEntity source) {

        if (!webApiProperties.cdmResults().enableCacheWarming()) {
            logger.info("Cache warming is disabled for CDM results");
            return new JobExecutionResource();
        }
        if (!SourceUtils.hasSourceDaimon(source, SourceDaimonEntity.DaimonType.Vocabulary)
                || !SourceUtils.hasSourceDaimon(source, SourceDaimonEntity.DaimonType.Results)) {
            logger.info(
                    "Caching isn't applied to sources without Vocabulary and Result schemas, source [{}] was omitted",
                    source.getSourceName());
            return new JobExecutionResource();
        }

        int resultDaimonPriority = getResultsDaimonPriority(source);
        if (!webApiProperties.cdmResults().achilles().enableCacheWarming() && resultDaimonPriority <= 0) {
            logger.info(
                    "Caching isn't applied to sources with result daimon priority <= 0 AND when the Achilles cache is disabled, source [{}] was omitted",
                    source.getSourceName());
            return new JobExecutionResource();
        }

        var jobName = getWarmCacheJobName(String.valueOf(source.getSourceId()), source.getSourceKey());
        var jobSteps = createCacheWarmingJobSteps(source, jobName);
        var job = createJob(jobName, jobSteps);
        return runJob(source.getSourceKey(), source.getSourceId(), jobName, job);
    }
}
