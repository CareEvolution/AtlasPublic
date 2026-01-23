package org.ohdsi.webapi.feanalysis.converter;

import org.ohdsi.webapi.feanalysis.domain.FeAnalysisEntity;
import org.ohdsi.webapi.feanalysis.dto.FeAnalysisShortDTO;
import org.ohdsi.webapi.util.ConversionUtils;

public class BaseFeAnalysisEntityToFeAnalysisDTOConverter {
    public static void convert(FeAnalysisEntity<?> source, FeAnalysisShortDTO target) {
        target.setType(source.getType());
        target.setName(source.getName());
        target.setId(source.getId());
        target.setDomain(source.getDomain());
        target.setDescription(source.getDescr());
        target.setStatType(source.getStatType());
        ConversionUtils.convertMetadata(source, target);
    }
}
