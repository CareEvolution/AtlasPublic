package org.ohdsi.webapi.cohortdefinition.converter;

import org.ohdsi.webapi.cohortdefinition.CohortDefinitionEntity;
import org.ohdsi.webapi.versioning.domain.CohortVersion;

public class CohortDefinitionToCohortVersionConverter {
    public static CohortVersion convert(CohortDefinitionEntity source) {
        CohortVersion target = new CohortVersion();
        target.setAssetId(source.getId());
        target.setDescription(source.getDescription());
        target.setAssetJson(source.getDetails().getExpression());

        return target;
    }
}
