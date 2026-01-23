package org.ohdsi.webapi.cohortanalysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;

import jakarta.ws.rs.NotFoundException;

import org.ohdsi.sql.SqlSplit;
import org.ohdsi.webapi.cohortdefinition.CohortDefinitionRepository;
import org.ohdsi.webapi.cohortdefinition.CohortDefinitionService;
import org.ohdsi.webapi.cohortresults.VisualizationDataRepository;
import org.ohdsi.webapi.job.GeneratesNotification;
import org.ohdsi.webapi.job.JobExecutionResource;
import org.ohdsi.webapi.job.JobService;
import org.ohdsi.webapi.model.results.Analysis;
import org.ohdsi.webapi.resourceutilities.StringResourceReader;
import org.ohdsi.webapi.source.SourceService;
import org.ohdsi.webapi.transactiontemplates.TransactionTemplates;
import org.ohdsi.webapi.util.PreparedStatementRenderer;
import org.ohdsi.webapi.util.SessionUtils;
import org.ohdsi.webapi.webapiproperties.WebApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import static org.ohdsi.webapi.util.SecurityUtils.whitelist;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;

@Component
public class CohortAnalysisService implements GeneratesNotification {
    private static final Logger log = LoggerFactory.getLogger(CohortAnalysisService.class);

    public static final String NAME = "cohortAnalysisJob";

    private final CohortDefinitionRepository cohortDefinitionRepository;
    private final CohortDefinitionService definitionService;
    private final HeraclesQueryBuilder heraclesQueryBuilder;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final SourceService sourceService;
    private final StringResourceReader resourceReader;
    private final TransactionTemplates transactionTemplates;
    private final VisualizationDataRepository visualizationDataRepository;
    private final WebApiProperties webApiProperties;
    private final JobService jobService;

    private final RowMapper<Analysis> analysisMapper = (rs, rowNum) -> {
        var analysis = new Analysis();
        mapAnalysis(analysis, rs, rowNum);
        return analysis;
    };

    private final RowMapper<CohortAnalysis> cohortAnalysisMapper = (rs, rowNum) -> {
        final var cohortAnalysis = new CohortAnalysis();
        mapAnalysis(cohortAnalysis, rs, rowNum);
        cohortAnalysis.setAnalysisComplete(rs.getInt(CohortAnalysis.ANALYSIS_COMPLETE) == 1);
        cohortAnalysis.setCohortDefinitionId(rs.getInt(CohortAnalysis.COHORT_DEFINITION_ID));
        cohortAnalysis.setLastUpdateTime(rs.getTimestamp(CohortAnalysis.LAST_UPDATE_TIME));
        return cohortAnalysis;
    };

    public CohortAnalysisService(
            CohortDefinitionRepository cohortDefinitionRepository,
            CohortDefinitionService definitionService,
            HeraclesQueryBuilder heraclesQueryBuilder,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            SourceService sourceService,
            StringResourceReader resourceReader,
            TransactionTemplates transactionTemplates,
            VisualizationDataRepository visualizationDataRepository,
            WebApiProperties webApiProperties,
            JobService jobService
            ) {

        this.cohortDefinitionRepository = cohortDefinitionRepository;
        this.definitionService = definitionService;
        this.heraclesQueryBuilder = heraclesQueryBuilder;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.sourceService = sourceService;
        this.resourceReader = resourceReader;
        this.transactionTemplates = transactionTemplates;
        this.visualizationDataRepository = visualizationDataRepository;
        this.webApiProperties = webApiProperties;
        this.jobService = jobService;
    }

    private void mapAnalysis(final Analysis analysis, final ResultSet rs, final int rowNum) throws SQLException {
        analysis.setAnalysisId(rs.getInt(Analysis.ANALYSIS_ID));
        analysis.setAnalysisName(rs.getString(Analysis.ANALYSIS_NAME));
        analysis.setStratum1Name(rs.getString(Analysis.STRATUM_1_NAME));
        analysis.setStratum2Name(rs.getString(Analysis.STRATUM_2_NAME));
        analysis.setStratum3Name(rs.getString(Analysis.STRATUM_3_NAME));
        analysis.setStratum4Name(rs.getString(Analysis.STRATUM_4_NAME));
        analysis.setStratum5Name(rs.getString(Analysis.STRATUM_5_NAME));
        analysis.setAnalysisType(rs.getString(Analysis.ANALYSIS_TYPE));
    }

    public List<Analysis> getCohortAnalyses() {
        var sqlPath = "cohortanalysis/sql/getCohortAnalyses.sql";
        var search = "ohdsi_database_schema";
        var replace = webApiProperties.appDb().schemaName();
        var psr = new PreparedStatementRenderer(null, sqlPath, search, replace);
        return jdbcTemplate.query(psr.getSql(), psr.getSetter(), this.analysisMapper);
    }

    public List<CohortAnalysis> getCohortAnalysesForCohortDefinition(final int id) {
        var sqlPath = "cohortanalysis/sql/getCohortAnalysesForCohort.sql";
        var tqName = "ohdsi_database_schema";
        var tqValue = webApiProperties.appDb().schemaName();
        var psr = new PreparedStatementRenderer(null, sqlPath, tqName, tqValue,
                "cohortDefinitionId", whitelist(id), SessionUtils.sessionId());
        return jdbcTemplate.query(psr.getSql(), psr.getSetter(), this.cohortAnalysisMapper);
    }

    public CohortSummary getCohortSummary(final int id) {

        CohortSummary summary = new CohortSummary();
        try {
            summary.setCohortDefinition(this.definitionService.getCohortDefinitionDto(whitelist(id)));
            summary.setAnalyses(this.getCohortAnalysesForCohortDefinition(id));
        } catch (Exception e) {
            log.error("unable to get cohort summary", e);
        }

        return summary;
    }

    public String getRunCohortAnalysisSql(CohortAnalysisTask task) {
        task.setSmallCellCount(webApiProperties.heracles().smallCellCount());
        return heraclesQueryBuilder.buildHeraclesAnalysisQuery(task);
    }

    public String[] getRunCohortAnalysisSqlBatch(CohortAnalysisTask task) {
        if (task != null) {
            task.setSmallCellCount(webApiProperties.heracles().smallCellCount());
            var sql = this.getRunCohortAnalysisSql(task);
            String[] stmts = null;
            if (log.isDebugEnabled()) {
                stmts = SqlSplit.splitSql(sql);
                for (int x = 0; x < stmts.length; x++) {
                    log.debug("Split SQL {} : {}", x, stmts[x]);
                }
            }
            return stmts;
        }
        return null;
    }

    public JobExecutionResource queueCohortAnalysisJob(CohortAnalysisTask task) throws Exception {
        if (task == null) {
            return null;
        }

        // source key comes from the client, we look it up here and hand it off to the tasklet
        var source = sourceService.findBySourceKey(task.getSourceKey());
        task.setSource(source);

        task.setSmallCellCount(webApiProperties.heracles().smallCellCount());
        JobParametersBuilder builder = new JobParametersBuilder();

        builder.addString("sourceKey", source.getSourceKey());
        builder.addString("cohortDefinitionIds", limitJobParams(Joiner.on(",").join(task.getCohortDefinitionIds())));
        builder.addString("analysisIds", limitJobParams(Joiner.on(",").join(task.getAnalysisIds())));
        if (task.getConditionConceptIds() != null && !task.getConditionConceptIds().isEmpty()) {
            builder.addString("conditionIds", limitJobParams(Joiner.on(",").join(task.getConditionConceptIds())));
        }
        if (task.getDrugConceptIds() != null && !task.getDrugConceptIds().isEmpty()) {
            builder.addString("drugIds", limitJobParams(Joiner.on(",").join(task.getDrugConceptIds())));
        }
        if (task.getMeasurementConceptIds() != null && !task.getMeasurementConceptIds().isEmpty()) {
            builder.addString("measurementIds", limitJobParams(Joiner.on(",").join(task.getMeasurementConceptIds())));
        }
        if (task.getObservationConceptIds() != null && !task.getObservationConceptIds().isEmpty()) {
            builder.addString("observationIds", limitJobParams(Joiner.on(",").join(task.getObservationConceptIds())));
        }
        if (task.getProcedureConceptIds() != null && !task.getProcedureConceptIds().isEmpty()) {
            builder.addString("procedureIds", limitJobParams(Joiner.on(",").join(task.getProcedureConceptIds())));
        }
        if (task.isRunHeraclesHeel()) {
            builder.addString("heraclesHeel", "true");
        }
        if (task.isCohortPeriodOnly()) {
            builder.addString("cohortPeriodOnly", "true");
        }

        if (StringUtils.hasText(task.getJobName())) {
            builder.addString("jobName", limitJobParams(task.getJobName()));
        }

        // clear analysis IDs from the generated set
        transactionTemplates.getTransactionTemplateRequiresNew().execute(status -> {
            var id = Integer.parseInt(task.getCohortDefinitionIds().getFirst());
            var optionalCohortDef = this.cohortDefinitionRepository.findById(id);
            var cohortDef = optionalCohortDef.orElseThrow(() ->
                    new NotFoundException(String.format("Cohort definition with id %d not found", id)));

            CohortAnalysisGenerationInfoEntity info = cohortDef.getCohortAnalysisGenerationInfoList().stream()
                    .filter(a -> a.getSourceId() == task.getSource().getSourceId())
                    .findFirst()
                    .orElseGet(() -> {
                        var genInfo = new CohortAnalysisGenerationInfoEntity();
                        genInfo.setSourceId(task.getSource().getSourceId());
                        genInfo.setCohortDefinition(cohortDef);
                        cohortDef.getCohortAnalysisGenerationInfoList().add(genInfo);
                        return genInfo;
                    });
            var analysisList = task.getAnalysisIds().stream().map(Integer::parseInt).toList();
            analysisList.forEach(info.getAnalysisIds()::remove);
            info.setLastExecution(Calendar.getInstance().getTime());
            info.setProgress(0);
            this.cohortDefinitionRepository.save(cohortDef);
            return null;
        });

        //TODO consider analysisId
        // TODO: Getting a log injection vulnerability warning here. The shouldn't be logging the whole
        // task here....should probably log individual parameters if needed.
        final var taskString = task.toString();
        final var jobParameters = builder.toJobParameters();
        log.info("Beginning run for cohort analysis task: {}", taskString);

        // TODO This is not testable.  Need to inject a factory...
        var tasklet = new CohortAnalysisTasklet(
                task,
                sourceService.getSourceJdbcTemplate(task.getSource()),
                transactionTemplates.getTransactionTemplate(),
                transactionTemplates.getTransactionTemplateRequiresNew(),
                this.visualizationDataRepository,
                this.cohortDefinitionRepository,
                objectMapper,
                heraclesQueryBuilder,
                resourceReader);

        var step = this.jobService.buildTaskletStep("cohortAnalysisStep", tasklet);
        var job = this.jobService.buildJob(NAME, List.of(step));
        return this.jobService.runJob(job, jobParameters);
    }

    @Override
    public String getJobName() {
        return NAME;
    }

    @Override
    public String getExecutionFoldingKey() {
        return "analysisIds";
    }

    private String limitJobParams(String param) {
        if (param.length() >= 250) {
            return param.substring(0, 245) + "...";
        }
        return param;
    }
}
