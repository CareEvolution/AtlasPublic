package org.ohdsi.webapi.conceptset.converter;

import org.apache.commons.lang3.StringUtils;
import org.ohdsi.webapi.conceptset.ConceptSet;
import org.ohdsi.webapi.conceptset.dto.ConceptSetDTO;
import org.springframework.stereotype.Component;

@Component
public class ConceptSetDTOToConceptSetConverter {

    public static ConceptSet convert(ConceptSetDTO dto) {

        ConceptSet conceptSet = new ConceptSet();
        conceptSet.setId(dto.getId());
        conceptSet.setName(StringUtils.trim(dto.getName()));
        conceptSet.setDescription(dto.getDescription());
        return conceptSet;
    }
}