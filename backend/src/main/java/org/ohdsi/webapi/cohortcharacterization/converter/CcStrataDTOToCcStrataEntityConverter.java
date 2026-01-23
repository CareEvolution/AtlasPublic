package org.ohdsi.webapi.cohortcharacterization.converter;

import org.ohdsi.analysis.Utils;
import org.ohdsi.webapi.cohortcharacterization.domain.CcStrataEntity;
import org.ohdsi.webapi.cohortcharacterization.dto.CcStrataDTO;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CcStrataDTOToCcStrataEntityConverter
        implements Converter<CcStrataDTO, CcStrataEntity> {

    public CcStrataEntity convert(CcStrataDTO source) {
        CcStrataEntity entity = new CcStrataEntity();
        entity.setId(source.getId());
        entity.setName(source.getName());
        entity.setExpressionString(Utils.serialize(source.getCriteria()));
        return entity;
    }
}
