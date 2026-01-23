package org.ohdsi.webapi.cohortcharacterization.converter;

import org.apache.commons.lang3.StringUtils;
import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.ohdsi.webapi.cohortcharacterization.domain.CohortCharacterizationEntity;
import org.ohdsi.webapi.cohortcharacterization.dto.CcShortDTO;
import org.ohdsi.webapi.util.ConversionUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CcToCcShortDTOConverter implements Converter<CohortCharacterizationEntity, CcShortDTO> {
    private final AuthorizationManager authorizationManager;
    public CcToCcShortDTOConverter(@Lazy AuthorizationManager authorizationManager) {
        this.authorizationManager = authorizationManager;
    }
    public CcShortDTO convert(CohortCharacterizationEntity source) {
        var target = new CcShortDTO();
        target.setName(StringUtils.trim(source.getName()));
        target.setDescription(source.getDescription());
        target.setId(source.getId());
        target.setHashCode(source.getHashCode());
        ConversionUtils.convertMetadataAndTags(source, target);
        authorizationManager.fillPermissions(source, target);
        return target;
    }
}
