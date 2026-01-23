package org.ohdsi.webapi.cohortcharacterization.converter;

import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.ohdsi.webapi.cohortcharacterization.domain.CohortCharacterizationEntity;
import org.ohdsi.webapi.cohortcharacterization.dto.CcExportDTO;
import org.ohdsi.webapi.cohortcharacterization.dto.CcParameterDTO;
import org.ohdsi.webapi.cohortcharacterization.dto.CcStrataDTO;
import org.ohdsi.webapi.cohortdefinition.dto.CohortDTO;
import org.ohdsi.webapi.feanalysis.dto.FeAnalysisDTO;
import org.ohdsi.webapi.util.ConversionUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CcToCcExportDTOConverter implements Converter<CohortCharacterizationEntity, CcExportDTO> {

    private final TempConverterUtils converterUtils;

    // The @Lazy is necessary. Otherwise the bean is newed up too late and doesn't get attached to the ConversionService, 
    // leading to "No converter found" exceptions
    public CcToCcExportDTOConverter(@Lazy TempConverterUtils converterUtils) {
        this.converterUtils = converterUtils;
    }

    @Override
    public CcExportDTO convert(CohortCharacterizationEntity source) {
        var exportDTO = new CcExportDTO();

        // Basics
        exportDTO.setName(StringUtils.trim(source.getName()));
        exportDTO.setDescription(source.getDescription());
        exportDTO.setId(source.getId());
        exportDTO.setHashCode(source.getHashCode());
        ConversionUtils.convertMetadataAndTags(source, exportDTO);

        // Parameters and strata
        Set<CcParameterDTO> convertedParameters = new TreeSet<>(
                (o1, o2) -> ObjectUtils.compare(o1.getId(), o2.getId()));
        convertedParameters.addAll(converterUtils.convertSet(source.getParameters(), CcParameterDTO.class));
        exportDTO.setParameters(convertedParameters);

        Set<CcStrataDTO> convertedStratas = new TreeSet<>((o1, o2) -> ObjectUtils.compare(o1.getId(), o2.getId()));
        convertedStratas.addAll(converterUtils.convertSet(source.getStratas(), CcStrataDTO.class));
        exportDTO.setStratas(convertedStratas);
        exportDTO.setStratifiedBy(source.getStratifiedBy());
        exportDTO.setStrataOnly(source.getStrataOnly());
        exportDTO.setStrataConceptSets(source.getStrataConceptSets());

        // Cohort definitions and feature analyses
        Set<CohortDTO> convertedCohortDTOs = new TreeSet<>((o1, o2) -> ObjectUtils.compare(o1.getId(), o2.getId()));
        convertedCohortDTOs.addAll(converterUtils.convertSet(source.getCohortDefinitions(), CohortDTO.class));
        exportDTO.setCohorts(convertedCohortDTOs);

        Set<FeAnalysisDTO> convertedFeAnalysisDTOs = new TreeSet<>(
                (o1, o2) -> ObjectUtils.compare(o1.getId(), o2.getId()));
        convertedFeAnalysisDTOs.addAll(converterUtils.convertSet(source.getFeatureAnalyses(), FeAnalysisDTO.class));
        exportDTO.setFeatureAnalyses(convertedFeAnalysisDTOs);

        return exportDTO;
    }
}
