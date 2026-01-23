package org.ohdsi.webapi.common.generation;

import org.ohdsi.webapi.Constants;
import org.ohdsi.webapi.cohortcharacterization.CreateCohortTableTasklet;
import org.ohdsi.webapi.cohortcharacterization.DropCohortTableListener;
import org.ohdsi.webapi.cohortcharacterization.GenerateLocalCohortTasklet;
import org.ohdsi.webapi.cohortdefinition.CohortDefinitionEntity;
import org.ohdsi.webapi.cohortgeneration.CohortGenerationService;
import org.ohdsi.webapi.generationcache.GenerationCacheHelper;
import org.ohdsi.webapi.generationtasks.GenerationTaskExceptionHandler;
import org.ohdsi.webapi.job.JobService;
import org.ohdsi.webapi.resourceutilities.StringResourceReader;
import org.ohdsi.webapi.source.SourceEntity;
import org.ohdsi.webapi.source.SourceService;
import org.ohdsi.webapi.sqlrender.SourceAwareSqlRender;
import org.ohdsi.webapi.util.SessionUtils;
import org.ohdsi.webapi.util.SourceUtils;
import org.ohdsi.webapi.util.TempTableCleanupManager;
import org.ohdsi.webapi.webapiproperties.WebApiProperties;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static org.ohdsi.webapi.Constants.Params.SESSION_ID;
import static org.ohdsi.webapi.Constants.Params.TARGET_TABLE;

@Component
public class GenerationUtils {
    private final TransactionTemplate transactionTemplate;
    private final CohortGenerationService cohortGenerationService;
    private final SourceService sourceService;
    private final JobService jobService;
    private final SourceAwareSqlRender sourceAwareSqlRender;
    private final StringResourceReader resourceReader;
    private final GenerationCacheHelper generationCacheHelper;
    private final WebApiProperties webApiProperties;

    public GenerationUtils(
            TransactionTemplate transactionTemplate,
            CohortGenerationService cohortGenerationService,
            SourceService sourceService,
            SourceAwareSqlRender sourceAwareSqlRender,
            JobService jobService,
            StringResourceReader resourceReader,
            GenerationCacheHelper generationCacheHelper,
            WebApiProperties webApiProperties) {
        this.transactionTemplate = transactionTemplate;
        this.cohortGenerationService = cohortGenerationService;
        this.sourceService = sourceService;
        this.sourceAwareSqlRender = sourceAwareSqlRender;
        this.jobService = jobService;
        this.resourceReader = resourceReader;
        this.generationCacheHelper = generationCacheHelper;
        this.webApiProperties = webApiProperties;
    }

    public static String getTempCohortTableName(String sessionId) {
        return Constants.TEMP_COHORT_TABLE_PREFIX + sessionId;
    }

    public Job buildJobForCohortBasedAnalysisTasklet(
            String analysisTypeName,
            SourceEntity source,
            JobParametersBuilder builder,
            JdbcTemplate jdbcTemplate,
            Function<ChunkContext, Collection<CohortDefinitionEntity>> cohortGetter,
            CancelableTasklet analysisTasklet
    ) {
        final var sessionId = SessionUtils.sessionId();
        addSessionParams(builder, sessionId);

        // TODO here and likely other places in this class.  This is not testable,
        //      should not be new-ing these.  Likely need an injected factory.
        var createCohortTableTasklet = new CreateCohortTableTasklet(
                jdbcTemplate,
                transactionTemplate,
                sourceService,
                sourceAwareSqlRender,
                resourceReader);

        var generateLocalCohortTasklet = new GenerateLocalCohortTasklet(
                transactionTemplate,
                sourceService.getSourceJdbcTemplate(source),
                cohortGenerationService,
                sourceService,
                cohortGetter,
                generationCacheHelper,
                webApiProperties.generationCache().useAsync());

        TempTableCleanupManager cleanupManager = new TempTableCleanupManager(
                        sourceService.getSourceJdbcTemplate(source),
                        transactionTemplate,
                        source.getSourceDialect(),
                        sessionId,
                        SourceUtils.getTempQualifier(source));
        var exceptionHandler = new GenerationTaskExceptionHandler(cleanupManager);

        var createCohortTableStep = this.jobService.buildTaskletStep(analysisTypeName + ".createCohortTable", createCohortTableTasklet);
        var generateLocalCohortStep = this.jobService.buildTaskletStep(analysisTypeName + ".generateCohort", generateLocalCohortTasklet);
        var generateAnalysisStep = this.jobService.buildTaskletStep(analysisTypeName + ".generate", analysisTasklet, exceptionHandler);

        var dropCohortTableListener = new DropCohortTableListener(
                jdbcTemplate,
                transactionTemplate,
                sourceService,
                sourceAwareSqlRender,
                resourceReader);

        return this.jobService.buildJob(analysisTypeName, List.of(createCohortTableStep, generateLocalCohortStep, generateAnalysisStep),
                List.of(dropCohortTableListener, new AutoremoveJobListener(jobService)));
    }

    protected void addSessionParams(JobParametersBuilder builder, String sessionId) {
        builder.addString(SESSION_ID, sessionId);
        builder.addString(TARGET_TABLE, GenerationUtils.getTempCohortTableName(sessionId));
    }
}
