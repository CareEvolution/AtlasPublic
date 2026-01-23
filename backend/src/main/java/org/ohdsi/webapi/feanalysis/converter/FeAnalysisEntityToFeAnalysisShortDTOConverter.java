package org.ohdsi.webapi.feanalysis.converter;

import org.ohdsi.webapi.feanalysis.domain.FeAnalysisEntity;
import org.ohdsi.webapi.feanalysis.dto.FeAnalysisShortDTO;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;


@Component
public class FeAnalysisEntityToFeAnalysisShortDTOConverter
        implements Converter<FeAnalysisEntity<?>, FeAnalysisShortDTO> {

    @Override
    public FeAnalysisShortDTO convert(FeAnalysisEntity<?> source) {
        FeAnalysisShortDTO target = new FeAnalysisShortDTO();
        BaseFeAnalysisEntityToFeAnalysisDTOConverter.convert(source, target);
        return target;
    }
}