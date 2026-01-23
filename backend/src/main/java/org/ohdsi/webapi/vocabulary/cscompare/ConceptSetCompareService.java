package org.ohdsi.webapi.vocabulary.cscompare;

import org.apache.commons.lang3.StringUtils;
import org.ohdsi.circe.vocabulary.ConceptSetExpression;
import org.ohdsi.circe.vocabulary.ConceptSetExpressionQueryBuilder;
import org.ohdsi.sql.SqlRender;
import org.ohdsi.sql.SqlTranslate;
import org.ohdsi.webapi.conceptset.ConceptSetComparison;
import org.ohdsi.webapi.resourceutilities.StringResourceReader;
import org.ohdsi.webapi.source.SourceDaimonEntity;
import org.ohdsi.webapi.source.SourceEntity;
import org.ohdsi.webapi.source.SourceHelper;
import org.ohdsi.webapi.source.SourceService;
import org.ohdsi.webapi.util.CancelableJdbcTemplate;
import org.ohdsi.webapi.webapiproperties.WebApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Function;

@Service
public class ConceptSetCompareService {
    private static final Logger log = LoggerFactory.getLogger(ConceptSetCompareService.class);

    private static final String TEMP_TABLE_NAME_TEMPLATE = "cs_code_%s";
    private static final int DEFAULT_BATCH_SIZE = 100;

    private final String tempTableCreateTemplateSql;
    private final String tempTableFillTemplateSql;
    private final String conceptSetQueryTemplateSql;
    private final String compareStatementSql;

    private final SourceHelper sourceHelper;
    private final SourceService sourceService;
    private final WebApiProperties webApiProperties;

    public ConceptSetCompareService(
            SourceHelper sourceHelper,
            SourceService sourceService,
            StringResourceReader resourceReader,
            WebApiProperties webApiProperties) {
        this.sourceHelper = sourceHelper;
        this.sourceService = sourceService;
        this.webApiProperties = webApiProperties;

        tempTableCreateTemplateSql = resourceReader.getText("vocabulary/sql/createConceptSetTempTable.sql");
        tempTableFillTemplateSql = resourceReader.getText("vocabulary/sql/fillConceptSetTempTable.sql");
        conceptSetQueryTemplateSql = resourceReader.getText("vocabulary/sql/getConceptByCodeAndVocabulary.sql");
        compareStatementSql = resourceReader.getText("vocabulary/sql/compareConceptSets.sql");
    }

    public static final RowMapper<ConceptSetComparison> CONCEPT_SET_COMPARISON_ROW_MAPPER = (rs, rowNum) -> {
        var csc = new ConceptSetComparison();
        csc.conceptId = rs.getLong("concept_id");
        csc.conceptIn1Only = rs.getLong("concept_in_1_only");
        csc.conceptIn2Only = rs.getLong("concept_in_2_only");
        csc.conceptIn1And2 = rs.getLong("concept_in_both_1_and_2");
        csc.conceptName = rs.getString("concept_name");
        csc.standardConcept = rs.getString("standard_concept");
        csc.invalidReason = rs.getString("invalid_reason");
        csc.conceptCode = rs.getString("concept_code");
        csc.domainId = rs.getString("domain_id");
        csc.vocabularyId = rs.getString("vocabulary_id");
        csc.validStartDate = rs.getDate("valid_start_date");
        csc.validEndDate = rs.getDate("valid_end_date");
        csc.conceptClassId = rs.getString("concept_class_id");
        return csc;
    };

    public Collection<ConceptSetComparison> compareConceptSets(final String sourceKey,
            final CompareArbitraryDto dto) throws Exception {
        final var csExpressionList = dto.compareTargets;
        if (csExpressionList.length != 2) {
            throw new IllegalArgumentException("You must specify two concept set expressions in order to use this method.");
        }

        final var source = sourceService.findBySourceKey(sourceKey);
        final var vocabSchema = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);

        final Function<JdbcTemplate, TransactionCallback<Collection<ConceptSetComparison>>> callbackFunction = jdbcTemplate -> (TransactionCallback<Collection<ConceptSetComparison>>) transactionStatus -> {
            try {
                final var csQuery1 = getQuery(csExpressionList[0], dto.types[0], source, jdbcTemplate);
                final var csQuery2 = getQuery(csExpressionList[1], dto.types[1], source, jdbcTemplate);

                // Insert the queries into the overall comparison script
                var sql = SqlRender.renderSql(compareStatementSql, new String[] { "cs1_expression", "cs2_expression" },
                        new String[] { csQuery1, csQuery2 });
                sql = SqlRender.renderSql(sql, new String[] { "vocabulary_database_schema" },
                        new String[] { vocabSchema });
                sql = SqlTranslate.translateSql(sql, source.getSourceDialect());

                // Execute the query
                return jdbcTemplate.query(sql, CONCEPT_SET_COMPARISON_ROW_MAPPER);
            } catch (Exception ex) {
                log.error("An error occurred during the comparing of concept sets", ex);
                throw ex;
            }
        };

        return executeInTransaction(source, callbackFunction);
    }

    private String getQuery(
            final ConceptSetExpression csExpression,
            final ExpressionType type,
            final SourceEntity source,
            final JdbcTemplate jdbcTemplate) {
        if (type == ExpressionType.CONCEPT_NAME_CODE_AND_VOCABULARY_ID_ONLY) {
            final var tempTableName = createTempTable(source, jdbcTemplate);
            fillTable(tempTableName, csExpression, jdbcTemplate);
            return StringUtils.replace(conceptSetQueryTemplateSql, "@temp_table", tempTableName);
        } else {
            final var builder = new ConceptSetExpressionQueryBuilder();
            return builder.buildExpressionQuery(csExpression);
        }
    }

    private String createTempTable(final SourceEntity source, final JdbcTemplate jdbcTemplate) {
        final var tableName = String.format(TEMP_TABLE_NAME_TEMPLATE,
                StringUtils.replace(UUID.randomUUID().toString(), "-", ""));

        var createStatement = SqlRender.renderSql(tempTableCreateTemplateSql, new String[] { "temp_table" },
                new String[] { tableName });
        createStatement = SqlTranslate.translateSql(createStatement, source.getSourceDialect());

        jdbcTemplate.execute(createStatement);

        return tableName;
    }

    private <T> T executeInTransaction(SourceEntity source, Function<JdbcTemplate, TransactionCallback<T>> callbackFunction) {
        var dataSource = sourceHelper.getDriverManagerDataSource(source);
        var jdbcTemplate = new CancelableJdbcTemplate(dataSource);
        jdbcTemplate.setSuppressApiException(webApiProperties.cdmDb().jdbcTemplateSuppressApiExceptions());
        var transactionManager = new DataSourceTransactionManager(dataSource);
        var transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        return transactionTemplate.execute(callbackFunction.apply(jdbcTemplate));
    }

    private void fillTable(
            final String tableName,
            final ConceptSetExpression expression,
            final JdbcTemplate jdbcTemplate) {
        if (expression.items.length > 0) {
            final var insertStatement = SqlRender.renderSql(
                    tempTableFillTemplateSql,
                    new String[] { "temp_table" },
                    new String[] { tableName });
            jdbcTemplate.batchUpdate(insertStatement,
                    Arrays.asList(expression.items),
                    DEFAULT_BATCH_SIZE,
                    (PreparedStatement ps, ConceptSetExpression.ConceptSetItem item) -> {
                        ps.setString(1, item.concept.conceptCode);
                        ps.setString(2, item.concept.vocabularyId);
                    });
        }
    }
}