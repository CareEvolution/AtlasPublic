/*
 * Copyright 2018 cknoll1.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ohdsi.webapi.cohortdefinition;

import org.ohdsi.sql.SqlRender;
import org.ohdsi.sql.SqlTranslate;
import org.ohdsi.webapi.resourceutilities.StringResourceReader;
import org.ohdsi.webapi.source.SourceDaimonEntity;
import org.ohdsi.webapi.source.SourceEntity;
import org.ohdsi.webapi.source.SourceService;
import org.ohdsi.webapi.util.SessionUtils;
import org.ohdsi.webapi.util.SourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

public class CleanupCohortTasklet implements Tasklet {
    private static final Logger log = LoggerFactory.getLogger(CleanupCohortTasklet.class);

    private final SourceService sourceService;
    private final TransactionTemplate transactionTemplate;

    private final String cleanupTemplateSql;

    public CleanupCohortTasklet(
            final SourceService sourceService,
            final StringResourceReader resourceReader,
            final TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
        this.sourceService = sourceService;

        this.cleanupTemplateSql = resourceReader.getText("cohortdefinition/sql/cleanupResults.sql");
    }

    private JdbcTemplate getSourceJdbcTemplate(SourceEntity sourceEntity) {
        var dataSource = new DriverManagerDataSource(sourceEntity.getSourceConnection());
        return new JdbcTemplate(dataSource);
    }

    private Integer doTask(ChunkContext chunkContext) {
        int sourcesUpdated = 0;

        var jobParams = chunkContext.getStepContext().getJobParameters();
        var cohortId = Integer.valueOf(jobParams.get("cohort_definition_id").toString());
        var sessionId = SessionUtils.sessionId();

        var resultsSources = this.sourceService.findAll().stream()
                .filter(source -> source.getDaimons().stream()
                        .anyMatch(daimon -> daimon.getDaimonType() == SourceDaimonEntity.DaimonType.Results))
                .toList();

        for (var source : resultsSources) {
            try {
                var resultSchema = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);
                var deleteSql = SqlRender.renderSql(cleanupTemplateSql,
                        new String[] { "results_database_schema", "cohort_definition_id" },
                        new String[] { resultSchema, cohortId.toString() });
                deleteSql = SqlTranslate.translateSql(deleteSql, source.getSourceDialect(), sessionId,
                        SourceUtils.getTempQualifier(source));

                getSourceJdbcTemplate(source).batchUpdate(
                        deleteSql.split(";")); // use batch update since SQL translation may produce multiple statements
                sourcesUpdated++;
            } catch (Exception e) {
                log.error("Error deleting results for cohort: {}, cause: {}", cohortId, e.getMessage());
            }
        }
        return sourcesUpdated;
    }

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) throws Exception {
        this.transactionTemplate.execute(status -> doTask(chunkContext));
        return RepeatStatus.FINISHED;
    }
}
