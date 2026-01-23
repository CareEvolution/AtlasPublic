package org.ohdsi.webapi.cohortcharacterization.converter;

import org.ohdsi.webapi.cohortcharacterization.domain.CcStrataEntity;
import org.ohdsi.webapi.cohortcharacterization.dto.CcStrataDTO;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CcStrataEntityToCcStrataDTOConverter
        implements Converter<CcStrataEntity, CcStrataDTO> {

    public CcStrataDTO convert(CcStrataEntity source) {
        var dto = new CcStrataDTO();
        dto.setId(source.getId());
        dto.setName(source.getName());
        dto.setCriteria(source.getCriteria());
        return dto;
    }
}
