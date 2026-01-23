package org.ohdsi.webapi.cohortdefinition.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import org.ohdsi.analysis.Utils;
import org.ohdsi.circe.cohortdefinition.CohortExpression;
import org.ohdsi.webapi.cohortdefinition.dto.CohortDTO;
import org.ohdsi.webapi.cohortdefinition.dto.CohortRawDTO;

import java.util.Objects;

public class CohortRawDTOToCohortDTOConverter {
    public static CohortDTO convert(CohortRawDTO source) {
        CohortDTO target = new CohortDTO();
        target.setDescription(source.getDescription());
        target.setName((source.getName()));
        target.setId(source.getId());

        CohortExpression expression = Objects.nonNull(source.getExpression())
                ? Utils.deserialize(source.getExpression(), new TypeReference<CohortExpression>() {
        })
                : null;
        target.setExpression(expression);
        target.setExpressionType(source.getExpressionType());
        target.setCreatedBy(source.getCreatedBy());
        target.setCreatedDate(source.getCreatedDate());
        target.setModifiedBy(source.getModifiedBy());
        target.setModifiedDate(source.getModifiedDate());
        target.setTags(source.getTags());

        return target;
    }
}
