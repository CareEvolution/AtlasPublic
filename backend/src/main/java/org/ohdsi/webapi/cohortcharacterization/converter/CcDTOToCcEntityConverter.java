package org.ohdsi.webapi.cohortcharacterization.converter;

import org.ohdsi.webapi.cohortcharacterization.domain.CohortCharacterizationEntity;
import org.ohdsi.webapi.cohortcharacterization.dto.CohortCharacterizationDTO;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CcDTOToCcEntityConverter extends BaseCcDTOToCcEntityConverter<CohortCharacterizationDTO> implements Converter<CohortCharacterizationDTO, CohortCharacterizationEntity> {

    // The @Lazy is necessary. Otherwise the bean is newed up too late and doesn't get attached to the ConversionService, 
    // leading to "No converter found" exceptions
    public CcDTOToCcEntityConverter(@Lazy TempConverterUtils converterUtils, @Lazy ConversionService conversionService) {
        super(converterUtils, conversionService);
    }

    @Override
    public CohortCharacterizationEntity convert(CohortCharacterizationDTO source) {
        var target = new CohortCharacterizationEntity();
        baseConvert(source, target);
        return target;
    }
}
