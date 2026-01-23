package org.ohdsi.webapi.conceptset.converter;

import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.ohdsi.webapi.conceptset.ConceptSet;
import org.ohdsi.webapi.conceptset.dto.ConceptSetDTO;
import org.ohdsi.webapi.util.ConversionUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ConceptSetToConceptSetDTOConverter implements Converter<ConceptSet, ConceptSetDTO> {

    private final AuthorizationManager authorizationManager;

    public ConceptSetToConceptSetDTOConverter(@Lazy AuthorizationManager authorizationManager) {
        this.authorizationManager = authorizationManager;
    }

    @Override
    public ConceptSetDTO convert(ConceptSet source) {
        ConceptSetDTO target = new ConceptSetDTO();
        target.setId(source.getId());
        target.setName(source.getName());
        target.setDescription(source.getDescription());
        ConversionUtils.convertMetadataAndTags(source, target);
        authorizationManager.fillPermissions(source, target);
        return target;
    }
}
