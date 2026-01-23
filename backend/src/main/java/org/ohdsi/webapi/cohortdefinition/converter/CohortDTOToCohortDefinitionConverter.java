package org.ohdsi.webapi.cohortdefinition.converter;

import org.ohdsi.analysis.Cohort;
import org.ohdsi.analysis.Utils;
import org.ohdsi.webapi.cohortdefinition.CohortDefinitionEntity;
import org.ohdsi.webapi.cohortdefinition.CohortDefinitionDetails;
import org.ohdsi.webapi.cohortdefinition.dto.CohortDTO;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CohortDTOToCohortDefinitionConverter extends BaseCohortDTOToCohortDefinitionConverter<CohortDTO> implements Converter<CohortDTO, CohortDefinitionEntity> {
    private String convertExpression(final Cohort source) {
        return Utils.serialize(source.getExpression());
    }

    @Override
    public CohortDefinitionEntity convert(CohortDTO source) {
        var target = super.baseConvert(source);
        if (source.getExpression() != null) {
            final CohortDefinitionDetails details = new CohortDefinitionDetails();
            final String expression = convertExpression(source);
            details.setExpression(expression);
            target.setDetails(details);
        }
        return target;
    }
}
