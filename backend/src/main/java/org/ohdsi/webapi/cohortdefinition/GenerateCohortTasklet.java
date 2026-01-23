/*
 * Copyright 2015 Observational Health Data Sciences and Informatics <OHDSI.org>.
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
import org.ohdsi.sql.SqlSplit;
import org.ohdsi.sql.SqlTranslate;
import org.ohdsi.webapi.common.generation.CancelableTasklet;
import org.ohdsi.webapi.generationcache.GenerationCacheHelper;
import org.ohdsi.webapi.resourceutilities.StringResourceReader;
import org.ohdsi.webapi.source.SourceService;
import org.ohdsi.webapi.util.CancelableJdbcTemplate;
import org.ohdsi.webapi.util.SessionUtils;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.transaction.support.TransactionTemplate;

import static org.ohdsi.webapi.Constants.Params.*;

public class GenerateCohortTasklet extends CancelableTasklet {
    private final String copyGenerationIntoCohortTableSql;

    private final GenerationCacheHelper generationCacheHelper;
    private final CohortDefinitionRepository cohortDefinitionRepository;
    private final SourceService sourceService;

    public GenerateCohortTasklet(
            final CancelableJdbcTemplate jdbcTemplate,
            TransactionTemplate transactionTemplate,
            final GenerationCacheHelper generationCacheHelper,
            final CohortDefinitionRepository cohortDefinitionRepository,
            final SourceService sourceService,
            StringResourceReader resourceReader) {
        super(LoggerFactory.getLogger(GenerateCohortTasklet.class), jdbcTemplate, transactionTemplate);
        this.generationCacheHelper = generationCacheHelper;
        this.cohortDefinitionRepository = cohortDefinitionRepository;
        this.sourceService = sourceService;

        this.copyGenerationIntoCohortTableSql = resourceReader.getText(
                "cohortdefinition/sql/copyGenerationIntoCohortTableSql.sql");
    }

    @Override
    protected String[] prepareQueries(ChunkContext chunkContext, CancelableJdbcTemplate jdbcTemplate) {
        var jobParams = chunkContext.getStepContext().getJobParameters();

        var cohortDefinitionId = Integer.valueOf(jobParams.get(COHORT_DEFINITION_ID).toString());
        var sourceId = Integer.parseInt(jobParams.get(SOURCE_ID).toString());
        var targetSchema = jobParams.get(TARGET_DATABASE_SCHEMA).toString();
        var sessionId = jobParams.getOrDefault(SESSION_ID, SessionUtils.sessionId()).toString();

        var cohortDefinition = cohortDefinitionRepository.findOneWithDetail(cohortDefinitionId);
        var source = sourceService.findBySourceId(sourceId);

        CohortGenerationRequestBuilder generationRequestBuilder = new CohortGenerationRequestBuilder(
                sessionId,
                targetSchema
        );

        int designHash = this.generationCacheHelper.computeHash(cohortDefinition.getDetails().getExpression());
        CohortGenerationUtils.insertInclusionRules(cohortDefinition, source, designHash, targetSchema, sessionId,
                jdbcTemplate);

        GenerationCacheHelper.CacheResult res = generationCacheHelper.computeCacheIfAbsent(
                cohortDefinition,
                source,
                generationRequestBuilder,
                (resId, sqls) -> generationCacheHelper.runCancelableCohortGeneration(jdbcTemplate, stmtCancel, sqls)
        );

        var sql = SqlRender.renderSql(
                copyGenerationIntoCohortTableSql,
                new String[] { RESULTS_DATABASE_SCHEMA, COHORT_DEFINITION_ID, DESIGN_HASH },
                new String[] { targetSchema, cohortDefinition.getId().toString(), res.getIdentifier().toString() }
        );
        sql = SqlTranslate.translateSql(sql, source.getSourceDialect());
        return SqlSplit.splitSql(sql);
    }
}
