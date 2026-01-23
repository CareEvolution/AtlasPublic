package org.ohdsi.webapi.db.migration;


import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.stereotype.Component;

@Component
public class V2_8_0_20190410103000__migratePathwayResults extends BaseJavaMigration {

    @Override
    public void migrate(Context arg0) throws Exception {

        // This migration is for pathway analysis.
        // We don't support this operations and our deployments post-date the time
        // prior to this migration.
        // I'm having this be a no-op.
    }
}
