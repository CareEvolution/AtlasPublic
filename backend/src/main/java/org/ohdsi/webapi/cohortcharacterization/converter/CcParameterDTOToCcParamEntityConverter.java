package org.ohdsi.webapi.cohortcharacterization.converter;

import org.ohdsi.webapi.cohortcharacterization.domain.CcParamEntity;
import org.ohdsi.webapi.cohortcharacterization.dto.CcParameterDTO;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CcParameterDTOToCcParamEntityConverter
        implements Converter<CcParameterDTO, CcParamEntity> {
    public CcParamEntity convert(final CcParameterDTO source) {
        final CcParamEntity result = new CcParamEntity();

        result.setName(source.getName());
        result.setId(source.getId());
        result.setValue(source.getValue());

        return result;
    }
}
