package org.ohdsi.webapi.db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.ohdsi.circe.helper.ResourceHelper;
import org.ohdsi.sql.SqlRender;
import org.ohdsi.sql.SqlSplit;
import org.ohdsi.webapi.source.SourceEntity;
import org.springframework.stereotype.Component;

@Component
public class V2_8_0_20191106092815__migrateEventFAType extends BaseJavaMigration {
    private final static String UPDATE_VALUE_SQL = ResourceHelper.GetResourceAsString(
            "/db/java_migration/V2_8_0_20191106092815__migrateEventFAType/updateFaType.sql");
    private final static String UPDATE_VALUE_IMPALA_SQL = ResourceHelper.GetResourceAsString(
            "/db/java_migration/V2_8_0_20191106092815__migrateEventFAType/updateFaTypeImpala.sql");

    @Override
    public void migrate(Context context) throws Exception {

        var connection = context.getConnection();

        var selectSql = "SELECT s.source_dialect, sd.table_qualifier FROM source s " +
                "JOIN source_daimon sd ON s.source_id = sd.source_id " +
                "WHERE sd.daimon_type = 2";

        try (PreparedStatement statement = connection.prepareStatement(selectSql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                String sourceDialect = resultSet.getString("source_dialect");
                String tableQualifier = resultSet.getString("table_qualifier");
                updateColumnValue(sourceDialect, tableQualifier, connection);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Migration failed", e);
        }
    }

    private void updateColumnValue(String sourceDialect, String tableQualifier, Connection connection) {
        String[] params = new String[] { "results_schema" };
        String[] values = new String[] { tableQualifier };
        String translatedSql;
        // Impala can't update non-kudu tables, so use special script with temp table
        if (SourceEntity.IMPALA_DATASOURCE.equals(sourceDialect)) {
            translatedSql = SqlRender.renderSql(UPDATE_VALUE_IMPALA_SQL, params, values);
        } else {
            translatedSql = SqlRender.renderSql(UPDATE_VALUE_SQL, params, values);
        }
        for (String sql : SqlSplit.splitSql(translatedSql)) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Migration failed", e);
            }
        }
    }
}