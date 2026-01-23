/*
 * Copyright 2017 Observational Health Data Sciences and Informatics <OHDSI.org>.
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
package org.ohdsi.webapi.cohortcharacterization;

import com.google.common.collect.ImmutableList;
import org.ohdsi.cohortcharacterization.CCQueryBuilder;
import org.ohdsi.sql.BigQuerySparkTranslate;
import org.ohdsi.sql.SqlSplit;
import org.ohdsi.sql.SqlTranslate;
import org.ohdsi.webapi.cohortcharacterization.converter.SerializedCcToCcConverter;
import org.ohdsi.webapi.cohortcharacterization.repository.AnalysisGenerationInfoEntityRepository;
import org.ohdsi.webapi.common.generation.AnalysisTasklet;
import org.ohdsi.webapi.model.DBMSType;
import org.ohdsi.webapi.authorization.repository.UserRepository;
import org.ohdsi.webapi.source.SourceService;
import org.ohdsi.webapi.util.CancelableJdbcTemplate;
import org.ohdsi.webapi.util.SourceUtils;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLException;

import static org.ohdsi.webapi.Constants.Params.*;

public class GenerateCohortCharacterizationTasklet extends AnalysisTasklet {
    private final CcService ccService;
    private final SourceService sourceService;
    private final UserRepository userRepository;

    public GenerateCohortCharacterizationTasklet(
            final CancelableJdbcTemplate jdbcTemplate,
            final TransactionTemplate transactionTemplate,
            final CcService ccService,
            final AnalysisGenerationInfoEntityRepository analysisGenerationInfoEntityRepository,
            final SourceService sourceService,
            final UserRepository userRepository
    ) {
        super(LoggerFactory.getLogger(GenerateCohortCharacterizationTasklet.class), jdbcTemplate, transactionTemplate,
                analysisGenerationInfoEntityRepository);
        this.ccService = ccService;
        this.sourceService = sourceService;
        this.userRepository = userRepository;
    }

    @Override
    protected String[] prepareQueries(ChunkContext chunkContext, CancelableJdbcTemplate jdbcTemplate) {
        var jobParams = chunkContext.getStepContext().getJobParameters();
        var cohortCharacterization = ccService.findByIdWithLinkedEntities(
                Long.valueOf(jobParams.get(COHORT_CHARACTERIZATION_ID).toString())
        );
        final Long jobId = chunkContext.getStepContext().getStepExecution().getJobExecution().getId();
        final var userEntity = userRepository.findByLogin(jobParams.get(JOB_AUTHOR).toString());
        var serializedDesign = new SerializedCcToCcConverter().convertToDatabaseColumn(cohortCharacterization);
        saveInfoWithinTheSeparateTransaction(jobId, serializedDesign, userEntity);
        final Integer sourceId = Integer.valueOf(jobParams.get(SOURCE_ID).toString());
        final var source = sourceService.findBySourceId(sourceId);
        final var cohortTable = jobParams.get(TARGET_TABLE).toString();
        final var sessionId = jobParams.get(SESSION_ID).toString();
        final var tempSchema = SourceUtils.getTempQualifier(source);
        final var fullCohortTable = tempSchema + "." + cohortTable;
        var ccQueryBuilder = new CCQueryBuilder(cohortCharacterization,
                fullCohortTable, sessionId,
                SourceUtils.getCdmQualifier(source), SourceUtils.getResultsQualifier(source),
                SourceUtils.getVocabularyQualifier(source), tempSchema, jobId, false, false);
        var sql = ccQueryBuilder.build();

        /*
         * There is an issue with temp tables on sql server: Temp tables scope is session or stored procedure.
         * To execute PreparedStatement sql server uses stored procedure <i>sp_executesql</i>
         * and this is the reason why multiple PreparedStatements cannot share the same local temporary table.
         *
         * On the other side, temp tables cannot be re-used in the same PreparedStatement, e.g. temp table cannot be created, used, dropped
         * and created again in the same PreparedStatement because sql optimizator detects object already exists and fails.
         * When is required to re-use temp table it should be separated to several PreparedStatements.
         *
         * An option to use global temp tables also doesn't work since such tables can be not supported / disabled.
         *
         * Therefore, there are two ways:
         * - either precisely group SQLs into statements so that temp tables aren't re-used in a single statement,
         * - or use ‘permanent temporary tables’
         *
         * The second option looks better since such SQL could be exported and executed manually,
         * which is not the case with the first option.
         */
        if (ImmutableList.of(DBMSType.MS_SQL_SERVER.getOhdsiDB(), DBMSType.PDW.getOhdsiDB())
                .contains(source.getSourceDialect())) {
            sql = sql
                    .replaceAll("#", tempSchema + "." + sessionId + "_")
                    .replaceAll("tempdb\\.\\.", "");
        }
        if (source.getSourceDialect().equals("spark")) {
            try {
                sql = BigQuerySparkTranslate.sparkHandleInsert(sql, source.getSourceConnection());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        final var translatedSql = SqlTranslate.translateSql(sql, source.getSourceDialect(), sessionId, tempSchema);
        return SqlSplit.splitSql(translatedSql);
    }
}
