package org.ohdsi.webapi.cohortcharacterization.converter;

import org.ohdsi.webapi.cohortcharacterization.domain.CcParamEntity;
import org.ohdsi.webapi.cohortcharacterization.dto.CcParameterDTO;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CcParamEntityToCcParameterDTOConverter
        implements Converter<CcParamEntity, CcParameterDTO> {
    public CcParameterDTO convert(final CcParamEntity source) {
        final CcParameterDTO dto = new CcParameterDTO();
        dto.setValue(source.getValue());
        dto.setName(source.getName());
        dto.setId(source.getId());
        return dto;
    }
}
