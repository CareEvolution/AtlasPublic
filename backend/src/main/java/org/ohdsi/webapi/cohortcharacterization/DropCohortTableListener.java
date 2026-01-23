package org.ohdsi.webapi.cohortcharacterization;

import org.apache.commons.lang3.StringUtils;
import org.ohdsi.sql.SqlTranslate;
import org.ohdsi.webapi.model.DBMSType;
import org.ohdsi.webapi.resourceutilities.StringResourceReader;
import org.ohdsi.webapi.source.SourceService;
import org.ohdsi.webapi.sqlrender.SourceAwareSqlRender;
import org.ohdsi.webapi.util.SourceUtils;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;

import static org.ohdsi.webapi.Constants.Params.SOURCE_ID;
import static org.ohdsi.webapi.Constants.Params.TARGET_TABLE;

public class DropCohortTableListener implements JobExecutionListener {
    private final String dropTableSql;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final SourceService sourceService;
    private final SourceAwareSqlRender sourceAwareSqlRender;

    public DropCohortTableListener(
            JdbcTemplate jdbcTemplate,
            TransactionTemplate transactionTemplate,
            SourceService sourceService,
            SourceAwareSqlRender sourceAwareSqlRender,
            StringResourceReader resourceReader) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.sourceService = sourceService;
        this.sourceAwareSqlRender = sourceAwareSqlRender;

        dropTableSql = resourceReader.getText("cohortcharacterizations/sql/dropCohortTable.sql");
    }

    private Object doTask(JobParameters parameters) {
        final var jobParameters = parameters.getParameters();
        final var sourceId = Integer.valueOf(jobParameters.get(SOURCE_ID).getValue().toString());
        final var targetTable = jobParameters.get(TARGET_TABLE).getValue().toString();
        final var sql = sourceAwareSqlRender.renderSql(sourceId, dropTableSql, TARGET_TABLE, targetTable);

        final var source = sourceService.findBySourceId(sourceId);
        final var resultsQualifier = SourceUtils.getResultsQualifier(source);
        final var tempQualifier = SourceUtils.getTempQualifier(source, resultsQualifier);
        var toRemove = SqlTranslate.translateSql(sql, source.getSourceDialect(), null, tempQualifier);

        if (Objects.equals(DBMSType.SPARK.getOhdsiDB(), source.getSourceDialect()) ||
                Objects.equals(DBMSType.HIVE.getOhdsiDB(), source.getSourceDialect())) {
            toRemove = StringUtils.remove(toRemove, ';');
        }
        jdbcTemplate.execute(toRemove);
        return null;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        // No action needed before the job starts
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        transactionTemplate.execute(transactionStatus -> doTask(jobExecution.getJobParameters()));
    }
}
