package org.ohdsi.webapi.cohortanalysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.NotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.ohdsi.sql.SqlSplit;
import org.ohdsi.webapi.cohortdefinition.CohortDefinitionRepository;
import org.ohdsi.webapi.cohortresults.CohortResultsAnalysisRunner;
import org.ohdsi.webapi.cohortresults.VisualizationDataRepository;
import org.ohdsi.webapi.resourceutilities.StringResourceReader;
import org.ohdsi.webapi.util.BatchStatementExecutorWithProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

import static org.ohdsi.webapi.util.SecurityUtils.whitelist;

import java.util.Calendar;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CohortAnalysisTasklet implements Tasklet {
    private static final Logger log = LoggerFactory.getLogger(CohortAnalysisTasklet.class);
    private final CohortAnalysisTask task;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final TransactionTemplate transactionTemplateRequiresNew;
    private final CohortResultsAnalysisRunner analysisRunner;
    private final CohortDefinitionRepository cohortDefinitionRepository;
    private final HeraclesQueryBuilder heraclesQueryBuilder;

    public CohortAnalysisTasklet(
            CohortAnalysisTask task,
            final JdbcTemplate jdbcTemplate,
            final TransactionTemplate transactionTemplate,
            final TransactionTemplate transactionTemplateRequiresNew,
            VisualizationDataRepository visualizationDataRepository,
            CohortDefinitionRepository cohortDefinitionRepository,
            final ObjectMapper objectMapper,
            HeraclesQueryBuilder heraclesQueryBuilder,
            StringResourceReader resourceReader) {
        this.task = task;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.transactionTemplateRequiresNew = transactionTemplateRequiresNew;
        this.heraclesQueryBuilder = heraclesQueryBuilder;

        // TODO This is not testable.  Need to inject a factory...
        this.analysisRunner = new CohortResultsAnalysisRunner(
                visualizationDataRepository,
                objectMapper,
                resourceReader);
        this.cohortDefinitionRepository = cohortDefinitionRepository;
    }

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) throws Exception {
        var successful = false;
        String failMessage = null;
        var cohortDefinitionId = Integer.parseInt(task.getCohortDefinitionIds().getFirst());
        this.transactionTemplate.execute(status -> {
            var optionalCohortDef = cohortDefinitionRepository.findById(cohortDefinitionId);
            if (optionalCohortDef.isEmpty()) {
                throw new NotFoundException(String.format("Cohort definition with id %d not found", cohortDefinitionId));
            }
            var cohortDef = optionalCohortDef.get();

            CohortAnalysisGenerationInfoEntity gi = cohortDef.getCohortAnalysisGenerationInfoList().stream()
                    .filter(a -> a.getSourceId() == task.getSource().getSourceId())
                    .findFirst()
                    .orElseGet(() -> {
                        CohortAnalysisGenerationInfoEntity genInfo = new CohortAnalysisGenerationInfoEntity();
                        genInfo.setSourceId(task.getSource().getSourceId());
                        genInfo.setCohortDefinition(cohortDef);
                        cohortDef.getCohortAnalysisGenerationInfoList().add(genInfo);
                        return genInfo;
                    });
            gi.setProgress(0);
            cohortDefinitionRepository.save(cohortDef);
            return gi;
        });
        try {
            final var cohortSql = heraclesQueryBuilder.buildHeraclesAnalysisQuery(task);
            var executor = new BatchStatementExecutorWithProgress(
                    SqlSplit.splitSql(cohortSql),
                    transactionTemplate,
                    jdbcTemplate);
            int[] ret = executor.execute(progress -> {

                transactionTemplateRequiresNew.execute(status -> {
                    var optionalCohortDef = cohortDefinitionRepository.findById(cohortDefinitionId);
                    if (optionalCohortDef.isEmpty()) {
                        throw new NotFoundException(String.format("Cohort definition with id %d not found", cohortDefinitionId));
                    }
                    var cohortDef = optionalCohortDef.get();

                    CohortAnalysisGenerationInfoEntity info = cohortDef.getCohortAnalysisGenerationInfoList().stream()
                            .filter(a -> a.getSourceId() == task.getSource().getSourceId())
                            .findFirst().orElseThrow(NotFoundException::new);
                    info.setProgress(progress);
                    cohortDefinitionRepository.save(cohortDef);
                    return null;
                });
            });

            if (log.isDebugEnabled()) {
                log.debug("Update count: {}", ret.length);
                log.debug("Warming up visualizations");
            }

            final int count = this.analysisRunner.warmupData(jdbcTemplate, task);
            if (log.isDebugEnabled()) {
                log.debug("Warmed up {} visualizations", count);
            }
            successful = true;
        } catch (final TransactionException | DataAccessException e) {
            log.error(whitelist(e));
            failMessage = StringUtils.left(e.getMessage(), 2000);
            throw e;// FAIL job status
        } finally {
            // add generated analysis IDs to cohort analysis generation info
            final String f_failMessage = failMessage; // assign final var to pass into lambda
            final boolean f_successful = successful; // assign final var to pass into lambda

            this.transactionTemplateRequiresNew.execute(status -> {
                var optionalCohortDef = cohortDefinitionRepository.findById(cohortDefinitionId);
                if (optionalCohortDef.isEmpty()) {
                    throw new NotFoundException(String.format("Cohort definition with id %d not found", cohortDefinitionId));
                }
                var cohortDef = optionalCohortDef.get();

                var info = cohortDef.getCohortAnalysisGenerationInfoList().stream()
                        .filter(a -> a.getSourceId() == task.getSource().getSourceId())
                        .findFirst().orElseThrow(NotFoundException::new);
                info.setExecutionDuration(
                        (int) (Calendar.getInstance().getTime().getTime() - info.getLastExecution().getTime()));

                if (f_successful) {
                    // merge existing analysisIds with analysis Ids generated.
                    Set<Integer> generatedIds = Stream.concat(
                                    task.getAnalysisIds().stream().map(Integer::parseInt),
                                    info.getAnalysisIds().stream())
                            .collect(Collectors.toSet());
                    info.setAnalysisIds(generatedIds);
                } else {
                    info.setFailMessage(f_failMessage);
                }
                this.cohortDefinitionRepository.save(cohortDef);
                return null;
            });
        }
        return RepeatStatus.FINISHED;
    }
}
