package org.ohdsi.webapi.cohortdefinition.converter;

import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.ohdsi.webapi.cohortdefinition.CohortDefinitionEntity;
import org.ohdsi.webapi.cohortdefinition.dto.CohortRawDTO;
import org.ohdsi.webapi.util.ConversionUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CohortDefinitionToCohortRawDTOConverter implements Converter<CohortDefinitionEntity, CohortRawDTO> {

    private final AuthorizationManager authorizationManager;

    public CohortDefinitionToCohortRawDTOConverter(@Lazy AuthorizationManager authorizationManager) {
        this.authorizationManager = authorizationManager;
    }

    @Override
    public CohortRawDTO convert(CohortDefinitionEntity cohortDefinitionEntity) {
        var target = new CohortRawDTO();
        target.setId(cohortDefinitionEntity.getId());
        target.setName(cohortDefinitionEntity.getName());
        target.setDescription(cohortDefinitionEntity.getDescription());
        target.setExpressionType(cohortDefinitionEntity.getExpressionType());
        if (cohortDefinitionEntity.getDetails() != null) {
            target.setExpression(cohortDefinitionEntity.getDetails().getExpression());
        }
        ConversionUtils.convertMetadataAndTags(cohortDefinitionEntity, target);
        authorizationManager.fillPermissions(cohortDefinitionEntity, target);
        return target;
    }
}
