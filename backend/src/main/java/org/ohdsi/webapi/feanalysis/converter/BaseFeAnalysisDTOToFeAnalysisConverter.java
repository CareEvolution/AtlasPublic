package org.ohdsi.webapi.feanalysis.converter;

import org.apache.commons.lang3.StringUtils;
import org.ohdsi.webapi.feanalysis.domain.FeAnalysisEntity;
import org.ohdsi.webapi.feanalysis.dto.FeAnalysisShortDTO;

public class BaseFeAnalysisDTOToFeAnalysisConverter {
    public static void  convert(FeAnalysisShortDTO source, FeAnalysisEntity<?> target) {
        target.setId(source.getId());
        target.setDescr(source.getDescription());
        target.setDomain(source.getDomain());
        target.setName(StringUtils.trim(source.getName()));
        target.setType(source.getType());
        target.setStatType(source.getStatType());
    }
}
