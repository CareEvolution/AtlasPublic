package org.ohdsi.webapi.sqlrender;

import static org.ohdsi.webapi.Constants.DEFAULT_DIALECT;

import java.util.Collections;
import java.util.Map;

import org.ohdsi.sql.SqlRender;
import org.ohdsi.sql.SqlTranslate;
import org.ohdsi.webapi.util.SessionUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SqlRenderService {

    public TranslatedStatement translateSQL(SourceStatement sourceStatement) {

        TranslatedStatement translated = new TranslatedStatement();
        if (sourceStatement == null) {
            return translated;
        }

        try {
            Map<String, String> parameters = sourceStatement.getParameters() == null
                    ? Collections.emptyMap()
                    : sourceStatement.getParameters();

            String renderedSQL = SqlRender.renderSql(
                    sourceStatement.getSql(),
                    parameters.keySet().toArray(new String[0]),
                    parameters.values().toArray(new String[0]));

            translated.setTargetSQL(translateSql(sourceStatement, renderedSQL));

            return translated;
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private static String translateSql(SourceStatement sourceStatement, String renderedSQL) {
        if (!StringUtils.hasText(sourceStatement.getTargetDialect()) || DEFAULT_DIALECT.equals(
                sourceStatement.getTargetDialect())) {
            return renderedSQL;
        }
        return SqlTranslate.translateSql(renderedSQL, sourceStatement.getTargetDialect(), SessionUtils.sessionId(),
                sourceStatement.getOracleTempSchema());
    }
}
