package org.ohdsi.webapi.cohortcharacterization;

import org.ohdsi.webapi.cohortdefinition.CohortDefinitionEntity;
import org.ohdsi.webapi.cohortdefinition.CohortGenerationRequestBuilder;
import org.ohdsi.webapi.cohortdefinition.CohortGenerationUtils;
import org.ohdsi.webapi.generationcache.GenerationCacheHelper;
import org.ohdsi.webapi.cohortgeneration.CohortGenerationService;
import org.ohdsi.webapi.source.SourceEntity;
import org.ohdsi.webapi.source.SourceService;
import org.ohdsi.webapi.util.*;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.StoppableTasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.ohdsi.webapi.Constants.Params.SOURCE_ID;
import static org.ohdsi.webapi.Constants.Params.TARGET_TABLE;

public class GenerateLocalCohortTasklet implements StoppableTasklet {
    private static final String COPY_CACHED_RESULTS = "INSERT INTO %s.%s (cohort_definition_id, subject_id, cohort_start_date, cohort_end_date) SELECT %s as cohort_definition_id, subject_id, cohort_start_date, cohort_end_date FROM (%s) r";

    protected TransactionTemplate transactionTemplate;
    private final CancelableJdbcTemplate cancelableJdbcTemplate;
    protected final CohortGenerationService cohortGenerationService;
    protected final SourceService sourceService;
    protected final Function<ChunkContext, Collection<CohortDefinitionEntity>> cohortGetter;
    private final GenerationCacheHelper generationCacheHelper;
    private final boolean useAsyncCohortGeneration;
    private final Set<StatementCancel> statementCancels = ConcurrentHashMap.newKeySet();
    private volatile boolean stopped = false;

    public GenerateLocalCohortTasklet(TransactionTemplate transactionTemplate,
            CancelableJdbcTemplate cancelableJdbcTemplate,
            CohortGenerationService cohortGenerationService,
            SourceService sourceService,
            Function<ChunkContext, Collection<CohortDefinitionEntity>> cohortGetter,
            GenerationCacheHelper generationCacheHelper,
            boolean useAsyncCohortGeneration) {
        this.transactionTemplate = transactionTemplate;
        this.cancelableJdbcTemplate = cancelableJdbcTemplate;
        this.cohortGenerationService = cohortGenerationService;
        this.sourceService = sourceService;
        this.cohortGetter = cohortGetter;
        this.generationCacheHelper = generationCacheHelper;
        this.useAsyncCohortGeneration = useAsyncCohortGeneration;
    }

    @Override
    public void stop() {
        try {
            stopped = true;
            for (StatementCancel statementCancel : statementCancels) {
                statementCancel.cancel();
            }
        } catch (SQLException ignored) {
        }
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        var jobParameters = chunkContext.getStepContext().getJobParameters();
        var source = sourceService.findBySourceId(Integer.valueOf(jobParameters.get(SOURCE_ID).toString()));
        var resultSchema = SourceUtils.getResultsQualifier(source);
        var targetTable = jobParameters.get(TARGET_TABLE).toString();

        var cohortDefinitions = cohortGetter.apply(chunkContext);

        if (useAsyncCohortGeneration) {
            List<CompletableFuture<?>> executions = cohortDefinitions.stream()
                    .map(cd ->
                            CompletableFuture.supplyAsync(() -> generateCohort(cd, source, resultSchema, targetTable),
                                    Executors.newSingleThreadExecutor()
                            )
                    ).collect(Collectors.toList());
            CompletableFuture.allOf(executions.toArray(new CompletableFuture[] {})).join();
        } else {
            CompletableFuture.runAsync(() ->
                            cohortDefinitions.stream().forEach(cd -> generateCohort(cd, source, resultSchema, targetTable)),
                    Executors.newSingleThreadExecutor()
            ).join();
        }

        return RepeatStatus.FINISHED;
    }

    private Object generateCohort(
            CohortDefinitionEntity cd,
            SourceEntity source,
            String resultSchema,
            String targetTable) {
        if (stopped) {
            return null;
        }
        var sessionId = SessionUtils.sessionId();
        var generationRequestBuilder = new CohortGenerationRequestBuilder(
                sessionId,
                resultSchema
        );

        int designHash = this.generationCacheHelper.computeHash(cd.getDetails().getExpression());
        CohortGenerationUtils.insertInclusionRules(cd, source, designHash, resultSchema, sessionId,
                cancelableJdbcTemplate);

        try {
            var stmtCancel = new StatementCancel();
            statementCancels.add(stmtCancel);
            GenerationCacheHelper.CacheResult res = generationCacheHelper.computeCacheIfAbsent(cd, source,
                    generationRequestBuilder, (resId, sqls) -> {
                        try {
                            generationCacheHelper.runCancelableCohortGeneration(cancelableJdbcTemplate, stmtCancel,
                                    sqls);
                        } finally {
                            // Usage of the same sessionId for all cohorts would cause issues in databases w/o real temp tables support
                            // And we cannot postfix existing sessionId with some index because SqlRender requires sessionId to be only 8 symbols long
                            // So, relying on TempTableCleanupManager.removeTempTables from GenerationTaskExceptionHandler is not an option
                            // That's why explicit TempTableCleanupManager call is defined
                            TempTableCleanupManager cleanupManager = new TempTableCleanupManager(
                                    cancelableJdbcTemplate,
                                    transactionTemplate,
                                    source.getSourceDialect(),
                                    sessionId,
                                    SourceUtils.getTempQualifier(source)
                            );
                            cleanupManager.cleanupTempTables();
                        }
                    });
            var sql = String.format(COPY_CACHED_RESULTS, SourceUtils.getTempQualifier(source), targetTable,
                    cd.getId(), res.getSql());
            cancelableJdbcTemplate.batchUpdate(stmtCancel, sql);
            statementCancels.remove(stmtCancel);
        } catch (StatementCancelException ignored) {
            // this exception must be caught to prevent "FAIL" status of the job
        }
        return null;
    }
}
