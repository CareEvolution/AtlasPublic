package org.ohdsi.webapi.cohortcharacterization.converter;

import org.ohdsi.webapi.cohortcharacterization.domain.CohortCharacterizationEntity;
import org.ohdsi.webapi.cohortcharacterization.dto.CcExportDTO;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CcExportDTOToCcEntityConverter extends BaseCcDTOToCcEntityConverter<CcExportDTO> implements Converter<CcExportDTO, CohortCharacterizationEntity> {

    // The @Lazy is necessary. Otherwise the bean is newed up too late and doesn't get attached to the ConversionService, 
    // leading to "No converter found" exceptions
    public CcExportDTOToCcEntityConverter(@Lazy TempConverterUtils converterUtils, @Lazy ConversionService conversionService) {
        super(converterUtils, conversionService);
    }

    @Override
    public CohortCharacterizationEntity convert(CcExportDTO source) {
        var target = new CohortCharacterizationEntity();
        baseConvert(source, target);
        return target;
    }
}
