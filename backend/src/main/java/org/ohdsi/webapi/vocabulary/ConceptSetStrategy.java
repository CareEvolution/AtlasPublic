package org.ohdsi.webapi.vocabulary;

import org.ohdsi.circe.vocabulary.ConceptSetExpression;
import org.ohdsi.circe.vocabulary.ConceptSetExpressionQueryBuilder;
import org.ohdsi.sql.SqlRender;
import org.ohdsi.webapi.source.SourceDaimonEntity;
import org.ohdsi.webapi.source.SourceEntity;
import org.ohdsi.webapi.util.PreparedStatementRenderer;

import java.util.Objects;
import java.util.function.Function;

public class ConceptSetStrategy implements StatementPrepareStrategy {
    private ConceptSetExpression expression;

    public ConceptSetStrategy(ConceptSetExpression expression) {
        this.expression = expression;
    }

    @Override
    public PreparedStatementRenderer prepareStatement(SourceEntity source, Function<String, String> queryModifier) {
        var builder = new ConceptSetExpressionQueryBuilder();
        var sql = builder.buildExpressionQuery(expression);
        if (Objects.nonNull(queryModifier)) {
            sql = queryModifier.apply(sql);
        }
        var tqName = "vocabulary_database_schema";
        var vocabularyTableQualifierName = "vocabularyTableQualifier";
        var tqValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);
        var vocabularyTableQualifierValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);

        String[] tableQualifierNames = { tqName, vocabularyTableQualifierName };
        String[] tableQualifierValues = { tqValue, vocabularyTableQualifierValue };
        sql = SqlRender.renderSql(sql, tableQualifierNames, tableQualifierValues);
        return new PreparedStatementRenderer(source, sql, tableQualifierNames, tableQualifierValues, null);
    }
}