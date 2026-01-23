package org.ohdsi.webapi.db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.ohdsi.webapi.cohortdefinition.CohortDefinitionDetails;

public class CohortDetailHashUpdater {
    public static void migrate(Connection connection) {

        var selectSql = "SELECT id, expression FROM cohort_definition_details";
        var updateSql = "UPDATE cohort_definition_details SET hash_code = ? WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(selectSql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String expression = resultSet.getString("expression");

                var cdDetails = new CohortDefinitionDetails();
                cdDetails.setExpression(expression);
                cdDetails.updateHashCode();

                try (PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {
                    updateStatement.setInt(1, cdDetails.getHashCode());
                    updateStatement.setInt(2, id);
                    updateStatement.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to update hash code for cohort_definition_details id: " + id, e);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Migration failed", e);
        }

    }
}
