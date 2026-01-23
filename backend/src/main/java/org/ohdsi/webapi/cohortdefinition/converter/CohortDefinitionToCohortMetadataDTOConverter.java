package org.ohdsi.webapi.cohortdefinition.converter;

import org.ohdsi.webapi.cohortdefinition.CohortDefinitionEntity;
import org.ohdsi.webapi.cohortdefinition.dto.CohortMetadataImplDTO;
import org.ohdsi.webapi.util.ConversionUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CohortDefinitionToCohortMetadataDTOConverter implements Converter<CohortDefinitionEntity, CohortMetadataImplDTO> {
    public CohortMetadataImplDTO convert(CohortDefinitionEntity cohortDefinitionEntity) {
        var target = new CohortMetadataImplDTO();
        target.setId(cohortDefinitionEntity.getId());
        target.setName(cohortDefinitionEntity.getName());
        target.setDescription(cohortDefinitionEntity.getDescription());
        ConversionUtils.convertMetadataAndTags(cohortDefinitionEntity, target);
        return target;
    }
}
