package org.ohdsi.webapi.feanalysis.converter;

import org.ohdsi.webapi.feanalysis.domain.FeAnalysisWithStringEntity;
import org.ohdsi.webapi.feanalysis.dto.FeAnalysisDTO;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class FeAnalysisWithStringEntityToFeAnalysisDTOConverter implements Converter<FeAnalysisWithStringEntity, FeAnalysisDTO> {

    @Override
    public FeAnalysisDTO convert(FeAnalysisWithStringEntity source) {
        var dto = new FeAnalysisDTO();
        BaseFeAnalysisEntityToFeAnalysisDTOConverter.convert(source, dto);
        dto.setDesign(source.getDesign());
        return dto;
    }
}
