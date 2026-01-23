package org.ohdsi.webapi.feanalysis.converter;

import org.ohdsi.analysis.cohortcharacterization.design.FeatureAnalysisAggregate;
import org.ohdsi.analysis.cohortcharacterization.design.StandardFeatureAnalysisDomain;
import org.ohdsi.webapi.feanalysis.domain.FeAnalysisAggregateEntity;
import org.ohdsi.webapi.feanalysis.dto.FeAnalysisAggregateDTO;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class FeatureAnalysisAggregateToDTOConverter
        implements Converter<FeatureAnalysisAggregate, FeAnalysisAggregateDTO> {

    @Override
    public FeAnalysisAggregateDTO convert(FeatureAnalysisAggregate source) {
        var dto = new FeAnalysisAggregateDTO();
        if (source instanceof FeAnalysisAggregateEntity) {
            dto.setId(source.getId());
        }
        dto.setDomain((StandardFeatureAnalysisDomain) source.getDomain());
        dto.setName(source.getName());
        dto.setExpression(source.getExpression());
        dto.setFunction(source.getFunction());
        dto.setJoinTable(source.getJoinTable());
        dto.setJoinType(source.getJoinType());
        dto.setJoinCondition(source.getJoinCondition());
        dto.setAdditionalColumns(source.getAdditionalColumns());
        if (source instanceof FeAnalysisAggregateEntity entity) {
            dto.setDefault(entity.isDefault());
        }
        dto.setMissingMeansZero(source.isMissingMeansZero());
        return dto;
    }
}
