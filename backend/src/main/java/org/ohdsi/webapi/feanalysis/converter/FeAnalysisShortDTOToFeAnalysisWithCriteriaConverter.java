package org.ohdsi.webapi.feanalysis.converter;

import org.ohdsi.analysis.cohortcharacterization.design.CcResultType;
import org.ohdsi.analysis.cohortcharacterization.design.StandardFeatureAnalysisType;
import org.ohdsi.webapi.feanalysis.domain.*;
import org.ohdsi.webapi.feanalysis.dto.FeAnalysisShortDTO;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class FeAnalysisShortDTOToFeAnalysisWithCriteriaConverter
        implements Converter<FeAnalysisShortDTO, FeAnalysisWithCriteriaEntity<?>> {

    @Override
    public FeAnalysisWithCriteriaEntity<?> convert(FeAnalysisShortDTO source) {
        var baseEntity = createResultObject(source);
        BaseFeAnalysisDTOToFeAnalysisConverter.convert(source, baseEntity);
        return baseEntity;
    }

    protected FeAnalysisWithCriteriaEntity<?> createResultObject(FeAnalysisShortDTO dto) {
        if (dto.getType() != StandardFeatureAnalysisType.CRITERIA_SET) {
            throw new IllegalArgumentException("Invalid Feature Analysis Type");
        }
        return Objects.equals(dto.getStatType(), CcResultType.PREVALENCE)
                ? new FeAnalysisWithPrevalenceCriteriaEntity()
                : new FeAnalysisWithDistributionCriteriaEntity();
    }
}
