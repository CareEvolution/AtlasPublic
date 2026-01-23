package org.ohdsi.webapi.feanalysis.converter;

import org.ohdsi.webapi.feanalysis.domain.FeAnalysisWithStringEntity;
import org.ohdsi.webapi.feanalysis.dto.FeAnalysisShortDTO;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class FeAnalysisShortDTOToFeAnalysisWithStringConverter implements Converter<FeAnalysisShortDTO, FeAnalysisWithStringEntity> {

    @Override
    public FeAnalysisWithStringEntity convert(FeAnalysisShortDTO source) {
        var target = new FeAnalysisWithStringEntity();
        BaseFeAnalysisDTOToFeAnalysisConverter.convert(source, target);
        return target;
    }
}
