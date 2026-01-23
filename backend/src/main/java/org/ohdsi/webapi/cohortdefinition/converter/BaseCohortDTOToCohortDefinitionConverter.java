package org.ohdsi.webapi.cohortdefinition.converter;

import org.apache.commons.lang3.StringUtils;
import org.ohdsi.webapi.cohortdefinition.CohortDefinitionEntity;
import org.ohdsi.webapi.cohortdefinition.dto.CohortMetadataDTO;

public abstract class BaseCohortDTOToCohortDefinitionConverter<V extends CohortMetadataDTO> {
    protected CohortDefinitionEntity baseConvert(V source) {
        var target = new CohortDefinitionEntity();
        target.setId(source.getId());
        target.setName(StringUtils.trim(source.getName()));
        target.setDescription(source.getDescription());
        return target;
    }
}
