package org.ohdsi.webapi.feanalysis.converter;

import org.ohdsi.analysis.cohortcharacterization.design.StandardFeatureAnalysisType;
import org.ohdsi.webapi.feanalysis.domain.FeAnalysisEntity;
import org.ohdsi.webapi.feanalysis.domain.FeAnalysisWithCriteriaEntity;
import org.ohdsi.webapi.feanalysis.domain.FeAnalysisWithStringEntity;
import org.ohdsi.webapi.feanalysis.dto.FeAnalysisDTO;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class FeAnalysisDTOToFeAnalysisEntityConverter implements Converter<FeAnalysisDTO, FeAnalysisEntity> {

    private final ConversionService conversionService;

    // The @Lazy is necessary. Otherwise the bean is newed up too late and doesn't get attached to the ConversionService, 
    // leading to "No converter found" exceptions
    public FeAnalysisDTOToFeAnalysisEntityConverter(@Lazy ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public FeAnalysisEntity convert(final FeAnalysisDTO source) {
        if (source.getType() == StandardFeatureAnalysisType.CUSTOM_FE || source.getType() == StandardFeatureAnalysisType.PRESET) {
            return conversionService.convert(source, FeAnalysisWithStringEntity.class);
        } else if (source.getType() == StandardFeatureAnalysisType.CRITERIA_SET) {
            return conversionService.convert(source, FeAnalysisWithCriteriaEntity.class);
        }
        throw new IllegalArgumentException("Only PRESET, CUSTOME_FE and CRITERIA_SET analyses are supported");
    }
}
