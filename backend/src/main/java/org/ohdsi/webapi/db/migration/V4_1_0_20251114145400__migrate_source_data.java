package org.ohdsi.webapi.db.migration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.ohdsi.webapi.util.EncryptionUtils;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component
@DependsOn("encryptionUtilsInitializer")
public class V4_1_0_20251114145400__migrate_source_data extends BaseJavaMigration {

    private record EncryptedSourceData(int id, String encryptedConnectionString, String encryptedUsername, String encryptedPassword) {}

    @Override
    public void migrate(Context context) throws Exception {

        var connection = context.getConnection();

        var encryptedSourceData = new ArrayList<EncryptedSourceData>();

        var sourceSelectSql = "SELECT s.source_id, s.source_connection, s.username, s.password FROM source s";
        try (PreparedStatement statement = connection.prepareStatement(sourceSelectSql);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                int id = resultSet.getInt("source_id");
                String connectionString = resultSet.getString("source_connection");
                String username = resultSet.getString("username");
                String password = resultSet.getString("password");
                encryptedSourceData.add(new EncryptedSourceData(id, EncryptionUtils.encrypt(connectionString),
                    EncryptionUtils.encrypt(username), EncryptionUtils.encrypt(password)));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Migration failed while encrypting source data", e);
        }

        var sourceUpdateSql = "UPDATE source SET source_connection = ?, username = ?, password = ? WHERE source_id = ?";
        try (PreparedStatement updateSourceStatement = connection.prepareStatement(sourceUpdateSql)) {
            for (var encryptedData : encryptedSourceData) {
                updateSourceStatement.setString(1, encryptedData.encryptedConnectionString());
                updateSourceStatement.setString(2, encryptedData.encryptedUsername());
                updateSourceStatement.setString(3, encryptedData.encryptedPassword());
                updateSourceStatement.setLong(4, encryptedData.id());
                updateSourceStatement.addBatch();
            }
            updateSourceStatement.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Migration failed while updating encrypted source data", e);
        }
    }

}
