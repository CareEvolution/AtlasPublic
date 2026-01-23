package org.ohdsi.webapi.feanalysis.converter;

import org.ohdsi.analysis.cohortcharacterization.design.StandardFeatureAnalysisType;
import org.ohdsi.webapi.feanalysis.dto.FeAnalysisDTO;
import org.ohdsi.webapi.feanalysis.domain.FeAnalysisWithStringEntity;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class FeAnalysisDTOToFeAnalysisWithStringConverter implements Converter<FeAnalysisDTO, FeAnalysisWithStringEntity> {

    @Override
    public FeAnalysisWithStringEntity convert(final FeAnalysisDTO source) {
        if (source.getType() != StandardFeatureAnalysisType.CUSTOM_FE && source.getType() != StandardFeatureAnalysisType.PRESET) {
            throw new IllegalArgumentException("Only PRESET and CUSTOME_FE analyses can have design of String type");
        }
        var baseEntity = new FeAnalysisWithStringEntity();
        BaseFeAnalysisDTOToFeAnalysisConverter.convert(source, baseEntity);
        baseEntity.setDesign(String.valueOf(source.getDesign()));
        return baseEntity;
    }

}
