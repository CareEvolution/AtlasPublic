package org.ohdsi.webapi.db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.stereotype.Component;

@Component
public class V2_7_2_20190515164044__hideSensitiveInfo extends BaseJavaMigration {

    @Override
    public void migrate(Context arg0) throws Exception {
        // This migration is for hiding sensitive data in the prediction and estimation analysis files.
        // We don't support these operations and our deployments post-date the time when these files
        // did not have sensitive data filtered out, so rather than rework this migration for SB3,
        // I'm having this be a no-op.
    }

}
