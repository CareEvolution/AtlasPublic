package org.ohdsi.webapi.sqlrender;

import static org.ohdsi.webapi.Constants.SqlSchemaPlaceholders.TEMP_DATABASE_SCHEMA_PLACEHOLDER;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for translating from OHDSI SQL into other supported SQL dialects. 
 */
@RestController
@RequestMapping("/sqlrender")
public class SqlRenderController {

    private final SqlRenderService sqlRenderService;

    public SqlRenderController(SqlRenderService sqlRenderService) {
        this.sqlRenderService = sqlRenderService;
    }

    /**
     * Translate an OHDSI SQL to a supported target SQL dialect
     *
     * @param sourceStatement
     *         JSON with parameters, source SQL, and target dialect
     * @return rendered and translated SQL
     */
    @PostMapping("/translate")
    public TranslatedStatement translateSQLFromSourceStatement(@RequestBody SourceStatement sourceStatement) {
        if (sourceStatement == null) {
            return new TranslatedStatement();
        }
        sourceStatement.setOracleTempSchema(TEMP_DATABASE_SCHEMA_PLACEHOLDER);
        return this.sqlRenderService.translateSQL(sourceStatement);
    }
}
