package org.ohdsi.webapi.cohortcharacterization.converter;

import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.ohdsi.webapi.cohortcharacterization.domain.CohortCharacterizationEntity;
import org.ohdsi.webapi.cohortcharacterization.dto.CcParameterDTO;
import org.ohdsi.webapi.cohortcharacterization.dto.CcStrataDTO;
import org.ohdsi.webapi.cohortcharacterization.dto.CohortCharacterizationDTO;
import org.ohdsi.webapi.cohortdefinition.dto.CohortMetadataImplDTO;
import org.ohdsi.webapi.feanalysis.dto.FeAnalysisShortDTO;
import org.ohdsi.webapi.util.ConversionUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;


@Component
public class CcToCcDTOConverter implements Converter<CohortCharacterizationEntity, CohortCharacterizationDTO> {

    private final TempConverterUtils converterUtils;

    // The @Lazy is necessary. Otherwise the bean is newed up too late and doesn't get attached to the ConversionService, 
    // leading to "No converter found" exceptions
    public CcToCcDTOConverter(@Lazy TempConverterUtils converterUtils) {
        this.converterUtils = converterUtils;
    }

    public CohortCharacterizationDTO convert(CohortCharacterizationEntity source) {
        var cohortCharacterizationDTO = new CohortCharacterizationDTO();

        // Basic stuff
        cohortCharacterizationDTO.setName(StringUtils.trim(source.getName()));
        cohortCharacterizationDTO.setDescription(source.getDescription());
        cohortCharacterizationDTO.setId(source.getId());
        cohortCharacterizationDTO.setHashCode(source.getHashCode());
        ConversionUtils.convertMetadataAndTags(source, cohortCharacterizationDTO);

        // Parameters and strata
        Set<CcParameterDTO> convertedParameters = new TreeSet<>(
                (o1, o2) -> ObjectUtils.compare(o1.getId(), o2.getId()));
        convertedParameters.addAll(converterUtils.convertSet(source.getParameters(), CcParameterDTO.class));
        cohortCharacterizationDTO.setParameters(convertedParameters);

        Set<CcStrataDTO> convertedStratas = new TreeSet<>((o1, o2) -> ObjectUtils.compare(o1.getId(), o2.getId()));
        convertedStratas.addAll(converterUtils.convertSet(source.getStratas(), CcStrataDTO.class));
        cohortCharacterizationDTO.setStratas(convertedStratas);
        cohortCharacterizationDTO.setStratifiedBy(source.getStratifiedBy());
        cohortCharacterizationDTO.setStrataOnly(source.getStrataOnly());
        cohortCharacterizationDTO.setStrataConceptSets(source.getStrataConceptSets());

        // Cohorts and feature analyses
        cohortCharacterizationDTO
                .setCohorts(converterUtils.convertSet(source.getCohortDefinitions(), CohortMetadataImplDTO.class));
        cohortCharacterizationDTO
                .setFeatureAnalyses(converterUtils.convertSet(source.getFeatureAnalyses(), FeAnalysisShortDTO.class));
        return cohortCharacterizationDTO;
    }
}
