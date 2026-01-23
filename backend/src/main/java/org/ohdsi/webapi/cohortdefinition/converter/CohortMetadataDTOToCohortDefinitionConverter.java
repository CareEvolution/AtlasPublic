package org.ohdsi.webapi.cohortdefinition.converter;

import org.ohdsi.webapi.cohortdefinition.CohortDefinitionEntity;
import org.ohdsi.webapi.cohortdefinition.dto.CohortMetadataImplDTO;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CohortMetadataDTOToCohortDefinitionConverter
        extends BaseCohortDTOToCohortDefinitionConverter<CohortMetadataImplDTO> implements Converter<CohortMetadataImplDTO, CohortDefinitionEntity> {
    @Override
    public CohortDefinitionEntity convert(CohortMetadataImplDTO source) {
        return super.baseConvert(source);
    }
}
