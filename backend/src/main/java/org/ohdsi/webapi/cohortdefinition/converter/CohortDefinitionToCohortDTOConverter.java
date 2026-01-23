package org.ohdsi.webapi.cohortdefinition.converter;

import org.ohdsi.webapi.cohortdefinition.CohortDefinitionEntity;
import org.ohdsi.webapi.cohortdefinition.dto.CohortDTO;
import org.ohdsi.webapi.util.ConversionUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CohortDefinitionToCohortDTOConverter implements Converter<CohortDefinitionEntity, CohortDTO> {

    @Override
    public CohortDTO convert(CohortDefinitionEntity cohortDefinitionEntity) {
        var target = new CohortDTO();
        target.setId(cohortDefinitionEntity.getId());
        target.setName(cohortDefinitionEntity.getName());
        target.setDescription(cohortDefinitionEntity.getDescription());
        target.setExpressionType(cohortDefinitionEntity.getExpressionType());
        if (cohortDefinitionEntity.getDetails() != null) {
            var expression = cohortDefinitionEntity.getDetails().getExpressionObject();
            target.setExpression(expression);
        }
        ConversionUtils.convertMetadataAndTags(cohortDefinitionEntity, target);
        return target;
    }
}
