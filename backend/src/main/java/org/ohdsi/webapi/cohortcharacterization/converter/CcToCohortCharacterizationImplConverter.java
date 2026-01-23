package org.ohdsi.webapi.cohortcharacterization.converter;

import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.ohdsi.webapi.cohortcharacterization.domain.CohortCharacterizationEntity;
import org.ohdsi.webapi.cohortcharacterization.dto.CcParameterDTO;
import org.ohdsi.webapi.cohortcharacterization.dto.CcStrataDTO;
import org.ohdsi.webapi.cohortcharacterization.specification.CohortCharacterizationImpl;
import org.ohdsi.webapi.cohortdefinition.dto.CohortDTO;
import org.ohdsi.webapi.feanalysis.dto.FeAnalysisDTO;
import org.ohdsi.webapi.util.ConversionUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CcToCohortCharacterizationImplConverter implements Converter<CohortCharacterizationEntity, CohortCharacterizationImpl> {

    private final TempConverterUtils converterUtils;

    // The @Lazy is necessary. Otherwise the bean is newed up too late and doesn't get attached to the ConversionService, 
    // leading to "No converter found" exceptions
    public CcToCohortCharacterizationImplConverter(@Lazy TempConverterUtils converterUtils) {
        this.converterUtils = converterUtils;
    }

    @Override
    public CohortCharacterizationImpl convert(CohortCharacterizationEntity source) {
        var target = new CohortCharacterizationImpl();

        // Basics
        target.setName(StringUtils.trim(source.getName()));
        target.setDescription(source.getDescription());
        target.setId(source.getId());
        target.setHashCode(source.getHashCode());
        ConversionUtils.convertMetadataAndTags(source, target);

        // Parameters and strata
        Set<CcParameterDTO> convertedParameters = new TreeSet<>(
                (o1, o2) -> ObjectUtils.compare(o1.getId(), o2.getId()));
        convertedParameters.addAll(converterUtils.convertSet(source.getParameters(), CcParameterDTO.class));
        target.setParameters(convertedParameters);

        Set<CcStrataDTO> convertedStratas = new TreeSet<>((o1, o2) -> ObjectUtils.compare(o1.getId(), o2.getId()));
        convertedStratas.addAll(converterUtils.convertSet(source.getStratas(), CcStrataDTO.class));
        target.setStratas(convertedStratas);

        target.setStratifiedBy(source.getStratifiedBy());
        target.setStrataOnly(source.getStrataOnly());
        target.setStrataConceptSets(source.getStrataConceptSets());

        // Cohort definitions and feature analyses
        Set<CohortDTO> convertedCohortDTOs = new TreeSet<>((o1, o2) -> ObjectUtils.compare(o1.getId(), o2.getId()));
        convertedCohortDTOs.addAll(converterUtils.convertSet(source.getCohortDefinitions(), CohortDTO.class));
        target.setCohorts(convertedCohortDTOs);

        Set<FeAnalysisDTO> convertedFeAnalysisDTOs = new TreeSet<>(
                (o1, o2) -> ObjectUtils.compare(o1.getId(), o2.getId()));
        convertedFeAnalysisDTOs.addAll(converterUtils.convertSet(source.getFeatureAnalyses(), FeAnalysisDTO.class));
        target.setFeatureAnalyses(convertedFeAnalysisDTOs);

        return target;
    }
}
