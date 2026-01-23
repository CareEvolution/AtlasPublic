package org.ohdsi.webapi.db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.stereotype.Component;

@Component
public class V2_8_0_20190520171430__cohortExpressionHashCode extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        var connection = context.getConnection();
        CohortDetailHashUpdater.migrate(connection);
    }
}
