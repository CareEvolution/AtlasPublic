package org.ohdsi.webapi.db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.stereotype.Component;

@Component
public class V2_6_0_20180807192421__cohortDetailsHashcodes extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        var connection = context.getConnection();
        CohortDetailHashUpdater.migrate(connection);
    }
}