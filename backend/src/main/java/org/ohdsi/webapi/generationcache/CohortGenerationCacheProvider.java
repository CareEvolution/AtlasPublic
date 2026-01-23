package org.ohdsi.webapi.generationcache;

import org.ohdsi.analysis.Utils;
import org.ohdsi.circe.cohortdefinition.CohortExpression;
import org.ohdsi.sql.SqlRender;
import org.ohdsi.sql.SqlSplit;
import org.ohdsi.sql.SqlTranslate;
import org.ohdsi.webapi.cohortdefinition.CohortDefinitionDetails;
import org.ohdsi.webapi.resourceutilities.StringResourceReader;
import org.ohdsi.webapi.source.SourceEntity;
import org.ohdsi.webapi.source.SourceService;
import org.ohdsi.webapi.util.PreparedStatementRenderer;
import org.ohdsi.webapi.util.SessionUtils;
import org.ohdsi.webapi.util.SourceUtils;
import org.ohdsi.webapi.util.StatementCancel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static org.ohdsi.webapi.Constants.Params.DESIGN_HASH;
import static org.ohdsi.webapi.Constants.Params.RESULTS_DATABASE_SCHEMA;

@Service
public class CohortGenerationCacheProvider implements GenerationCacheProvider {
    private static final Logger log = LoggerFactory.getLogger(CohortGenerationCacheProvider.class);

    private static final String CACHE_VALIDATION_TIME = "Checksum of Generation cache for designHash = {} has been calculated in {} milliseconds";
    private static final String COHORT_CHECKSUM_SQL_PATH = "generationcache/cohort/resultsChecksum.sql";

    private final String cohortResultsSql;
    private final String cleanupSql;

    private final SourceService sourceService;

    public CohortGenerationCacheProvider(
            SourceService sourceService,
            StringResourceReader resourceReader) {
        this.sourceService = sourceService;

        cohortResultsSql = resourceReader.getText("generationcache/cohort/results.sql");
        cleanupSql = resourceReader.getText("generationcache/cohort/cleanup.sql");
    }

    @Override
    public boolean supports(CacheableGenerationType type) {
        return Objects.equals(type, CacheableGenerationType.COHORT);
    }

    @Override
    public Integer getDesignHash(String design) {
        // remove elements from object that do not determine results output (names, descriptions, etc)
        var cleanExpression = CohortExpression.fromJson(design);
        cleanExpression.title = null;
        cleanExpression.inclusionRules.forEach((rule) -> {
            rule.name = null;
            rule.description = null;
        });

        var cohortDetails = new CohortDefinitionDetails();
        cohortDetails.setExpression(Utils.serialize(cleanExpression));
        return cohortDetails.calculateHashCode();
    }

    @Override
    public String getResultsChecksum(SourceEntity source, Integer designHash) {
        long startTime = System.currentTimeMillis();
        var psr = new PreparedStatementRenderer(
                source,
                COHORT_CHECKSUM_SQL_PATH,
                "@" + RESULTS_DATABASE_SCHEMA,
                SourceUtils.getResultsQualifier(source),
                DESIGN_HASH,
                designHash,
                SessionUtils.sessionId()
        );
        var checksum = sourceService.getSourceJdbcTemplate(source).queryForObject(psr.getSql(), psr.getOrderedParams(),
                String.class);
        log.info(CACHE_VALIDATION_TIME, designHash, System.currentTimeMillis() - startTime);
        return checksum;
    }

    @Override
    public String getResultsSql(Integer designHash) {
        return SqlRender.renderSql(
                cohortResultsSql,
                new String[] { DESIGN_HASH },
                new String[] { designHash.toString() }
        );
    }

    @Override
    public void remove(SourceEntity source, Integer designHash) {
        var sql = SqlRender.renderSql(
                cleanupSql,
                new String[] { RESULTS_DATABASE_SCHEMA, DESIGN_HASH },
                new String[] { SourceUtils.getResultsQualifier(source), designHash.toString() }
        );
        sql = SqlTranslate.translateSql(sql, source.getSourceDialect());

        try {
            // StatementCancel parameter is needed for calling batchUpdate of CancelableJdbcTemplate class
            // Without StatementCancel parameter JdbcTemplate.batchUpdate will be used.
            // JdbcTemplate incorrectly determines the support of batch update for impala datasource
            sourceService.getSourceJdbcTemplate(source).batchUpdate(new StatementCancel(), SqlSplit.splitSql(sql));
        } catch (final Exception e) {
            // if source is unavailable it throws exception that prevents source from being deleted.
            // ignore exception and proceed with deletion.
            log.warn("Cannot remove generation caches from source {}", source.getSourceId());
        }
    }
}
