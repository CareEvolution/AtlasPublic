package org.ohdsi.webapi.cohortcharacterization;

import org.ohdsi.sql.SqlSplit;
import org.ohdsi.sql.SqlTranslate;
import org.ohdsi.webapi.resourceutilities.StringResourceReader;
import org.ohdsi.webapi.source.SourceService;
import org.ohdsi.webapi.sqlrender.SourceAwareSqlRender;
import org.ohdsi.webapi.util.SourceUtils;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;

import static org.ohdsi.webapi.Constants.Params.SOURCE_ID;
import static org.ohdsi.webapi.Constants.Params.TARGET_TABLE;

public class CreateCohortTableTasklet implements Tasklet {
    private final String createCohortSql;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final SourceService sourceService;
    private final SourceAwareSqlRender sourceAwareSqlRender;

    public CreateCohortTableTasklet(
            JdbcTemplate jdbcTemplate,
            TransactionTemplate transactionTemplate,
            SourceService sourceService,
            SourceAwareSqlRender sourceAwareSqlRender,
            StringResourceReader resourceReader) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.sourceService = sourceService;
        this.sourceAwareSqlRender = sourceAwareSqlRender;

        createCohortSql = resourceReader.getText("cohortcharacterizations/sql/createCohortTable.sql");
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        transactionTemplate.execute(transactionStatus -> doTask(chunkContext));
        return RepeatStatus.FINISHED;
    }

    private Object doTask(ChunkContext chunkContext) {
        final var jobParameters = chunkContext.getStepContext().getJobParameters();
        final var sourceId = Integer.valueOf(jobParameters.get(SOURCE_ID).toString());
        final var targetTable = jobParameters.get(TARGET_TABLE).toString();
        final var sql = sourceAwareSqlRender.renderSql(sourceId, createCohortSql, TARGET_TABLE, targetTable);

        final var source = sourceService.findBySourceId(sourceId);
        final var resultsQualifier = SourceUtils.getResultsQualifier(source);
        final var tempQualifier = SourceUtils.getTempQualifier(source, resultsQualifier);
        final var translatedSql = SqlTranslate.translateSql(sql, source.getSourceDialect(), null, tempQualifier);
        Arrays.stream(SqlSplit.splitSql(translatedSql)).forEach(jdbcTemplate::execute);

        return null;
    }
}