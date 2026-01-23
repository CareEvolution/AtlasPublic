package org.ohdsi.webapi.cohortgeneration;

import org.ohdsi.webapi.generationtasks.GenerationStatus;
import org.ohdsi.webapi.cohortdefinition.*;
import org.ohdsi.webapi.generationcache.GenerationCacheHelper;
import org.ohdsi.webapi.generationtasks.GenerationTaskExceptionHandler;
import org.ohdsi.webapi.job.GeneratesNotification;
import org.ohdsi.webapi.job.JobExecutionResource;
import org.ohdsi.webapi.job.JobService;
import org.ohdsi.webapi.resourceutilities.StringResourceReader;
import org.ohdsi.webapi.Constants;
import org.ohdsi.webapi.IExecutionInfo;
import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.ohdsi.webapi.authorization.converter.UserDtoToUserEntity;
import org.ohdsi.webapi.source.SourceEntity;
import org.ohdsi.webapi.source.SourceService;
import org.ohdsi.webapi.transactiontemplates.TransactionTemplates;
import org.ohdsi.webapi.util.SessionUtils;
import org.ohdsi.webapi.util.SourceUtils;
import org.ohdsi.webapi.util.TempTableCleanupManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.NotFoundException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.ohdsi.webapi.Constants.GENERATE_COHORT;
import static org.ohdsi.webapi.Constants.Params.*;

@Component
public class CohortGenerationService implements GeneratesNotification {
    final Logger log = LoggerFactory.getLogger(getClass());

    private final AuthorizationManager authorizationManager;
    private final CohortDefinitionRepository cohortDefinitionRepository;
    private final CohortGenerationInfoRepository cohortGenerationInfoRepository;
    private final JobService jobService;
    private final SourceService sourceService;
    private final TransactionTemplates transactionTemplates;
    private final GenerationCacheHelper generationCacheHelper;
    private final StringResourceReader resourceReader;

    public static final List<GenerationStatus> INVALIDATE_STATUSES = new ArrayList<>() {
        {
            add(GenerationStatus.PENDING);
            add(GenerationStatus.RUNNING);
        }
    };

    public CohortGenerationService(
            AuthorizationManager authorizationManager,
            CohortDefinitionRepository cohortDefinitionRepository,
            CohortGenerationInfoRepository cohortGenerationInfoRepository,
            JobService jobService,
            SourceService sourceService,
            TransactionTemplates transactionTemplates,
            GenerationCacheHelper generationCacheHelper,
            StringResourceReader resourceReader) {
        this.authorizationManager = authorizationManager;
        this.cohortDefinitionRepository = cohortDefinitionRepository;
        this.cohortGenerationInfoRepository = cohortGenerationInfoRepository;
        this.jobService = jobService;
        this.sourceService = sourceService;
        this.transactionTemplates = transactionTemplates;
        this.generationCacheHelper = generationCacheHelper;
        this.resourceReader = resourceReader;
    }

    public JobExecutionResource generateCohortViaJob(int cohortID, String sourceKey) {
        var source = sourceService.findBySourceKey(sourceKey);
        if (source == null) {
            throw new NotFoundException(String.format("There is no source with key = %s.", sourceKey));
        }

        var cohortDefinition = transactionTemplates.getTransactionTemplate().execute(transactionStatus -> {
            return updateCohortDefinitionGenerationInfoList(cohortID, source);
        });

        return runGenerateCohortJob(cohortDefinition, source);
    }

    private CohortDefinitionEntity updateCohortDefinitionGenerationInfoList(int cohortID, SourceEntity source) {
        var optionalDef = this.cohortDefinitionRepository.findById(cohortID);
        if (optionalDef.isEmpty()) {
            throw new NotFoundException(String.format("There is no cohort definition with id = %d.", cohortID));
        }

        var cohortDefinition = optionalDef.get();
        var info = cohortDefinition.getGenerationInfoList().stream()
                .filter(val -> Objects.equals(val.getId().getSourceId(), source.getSourceId())).findFirst()
                .orElse(new CohortGenerationInfo(cohortDefinition, source.getSourceId()));

        var currentUser = authorizationManager.getCurrentUser();
        info.setCreatedBy(UserDtoToUserEntity.convert(currentUser));
        cohortDefinition.getGenerationInfoList().add(info);
        info.setStatus(GenerationStatus.PENDING)
                .setStartTime(Calendar.getInstance().getTime());

        cohortDefinitionRepository.save(cohortDefinition);

        return cohortDefinition;
    }

    private Job buildGenerateCohortJob(
        CohortDefinitionEntity cohortDefinition,
        SourceEntity source,
        JobParameters jobParameters) {

        log.info("Building generate cohort job for cohort definition id: {}", cohortDefinition.getId());

        var generateTasklet = new GenerateCohortTasklet(
                sourceService.getSourceJdbcTemplate(source),
                transactionTemplates.getTransactionTemplate(),
                generationCacheHelper,
                cohortDefinitionRepository,
                sourceService,
                resourceReader);

        var exceptionHandler = new GenerationTaskExceptionHandler(
                new TempTableCleanupManager(sourceService.getSourceJdbcTemplate(source),
                        transactionTemplates.getTransactionTemplate(),
                        source.getSourceDialect(),
                        jobParameters.getString(SESSION_ID),
                        SourceUtils.getTempQualifierOrNull(source)));

        var listener = new GenerationJobExecutionListener(sourceService, cohortDefinitionRepository,
                transactionTemplates.getTransactionTemplateRequiresNew(),
                sourceService.getSourceJdbcTemplate(source));

        Step generateCohortStep = this.jobService.buildTaskletStep("cohortDefinition.generateCohort", 
            generateTasklet, exceptionHandler);

        return this.jobService.buildJob(GENERATE_COHORT, List.of(generateCohortStep), List.of(listener));
    }

    private void invalidateExecutions(List<CohortGenerationInfo> executionInfoList) {
        for (CohortGenerationInfo executionInfo : executionInfoList) {
            invalidateExecution(executionInfo);
        }
    }

    private void invalidateExecution(IExecutionInfo<?> executionInfo) {
        executionInfo.setIsValid(false)
                .setStatus(GenerationStatus.COMPLETE)
                .setMessage("Invalidated by system");
    }

    private JobExecutionResource runGenerateCohortJob(
            CohortDefinitionEntity cohortDefinition,
            SourceEntity source) {
        final JobParametersBuilder jobParametersBuilder = getJobParametersBuilder(source, cohortDefinition);
        var job = buildGenerateCohortJob(cohortDefinition, source, jobParametersBuilder.toJobParameters());
        return jobService.runJob(job, jobParametersBuilder.toJobParameters());
    }

    private JobParametersBuilder getJobParametersBuilder(
            SourceEntity source, CohortDefinitionEntity cohortDefinition) {
        var builder = new JobParametersBuilder();
        builder.addString(JOB_NAME, String.format("Generating cohort %d : %s (%s)", cohortDefinition.getId(),
                source.getSourceName(), source.getSourceKey()));
        builder.addString(TARGET_DATABASE_SCHEMA, SourceUtils.getResultsQualifier(source));
        builder.addString(SESSION_ID, SessionUtils.sessionId());
        builder.addString(COHORT_DEFINITION_ID, String.valueOf(cohortDefinition.getId()));
        builder.addString(SOURCE_ID, String.valueOf(source.getSourceId()));
        builder.addString(GENERATE_STATS, Boolean.TRUE.toString());
        return builder;
    }

    @PostConstruct
    public void init() {
        invalidateCohortGenerations();
    }

    private void invalidateCohortGenerations() {
        transactionTemplates.getTransactionTemplateRequiresNew().execute(status -> {
            List<CohortGenerationInfo> executions = cohortGenerationInfoRepository.findByStatusIn(INVALIDATE_STATUSES);
            invalidateExecutions(executions);
            cohortGenerationInfoRepository.saveAll(executions);
            return null;
        });
    }

    @Override
    public String getJobName() {
        return GENERATE_COHORT;
    }

    @Override
    public String getExecutionFoldingKey() {
        return COHORT_DEFINITION_ID;
    }

    public void cancelGenerateCohort(final int id, final String sourceKey) {
        final var source = Optional.ofNullable(sourceService.findBySourceKey(sourceKey))
                .orElseThrow(NotFoundException::new);
        transactionTemplates.getTransactionTemplateRequiresNew().execute(status -> {
            var optionalDef = cohortDefinitionRepository.findById(id);
            if (!optionalDef.isPresent()) {
                throw new NotFoundException(String.format("Cohort definition with id %d not found", id));
            }
            var currentDefinition = optionalDef.get();
            var info = findBySourceId(currentDefinition.getGenerationInfoList(),
                    source.getSourceId());
            if (info == null) {
                throw new NotFoundException(String.format("Cohort generation info for cohort definition id %d and source id %d not found",
                        id, source.getSourceId()));
            }
            invalidateExecution(info);
            cohortDefinitionRepository.save(currentDefinition);
            return null;
        });

        jobService.cancelJobExecution(e -> {
            var parameters = e.getJobParameters();
            var jobName = e.getJobInstance().getJobName();
            return Objects.equals(parameters.getString(COHORT_DEFINITION_ID), Integer.toString(id))
                    && Objects.equals(parameters.getString(SOURCE_ID), Integer.toString(source.getSourceId()))
                    && Objects.equals(Constants.GENERATE_COHORT, jobName);
        });
    }

    private CohortGenerationInfo findBySourceId(Set<CohortGenerationInfo> infoList, Integer sourceId) {
        for (CohortGenerationInfo info : infoList) {
            if (info.getId().getSourceId().equals(sourceId)) {
                return info;
            }
        }
        return null;
    }

}
