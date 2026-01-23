package org.ohdsi.webapi.cohortcharacterization.converter;

import org.apache.commons.lang3.StringUtils;
import org.ohdsi.analysis.Utils;
import org.ohdsi.analysis.cohortcharacterization.design.CcResultType;
import org.ohdsi.webapi.cohortcharacterization.domain.CcStrataConceptSetEntity;
import org.ohdsi.webapi.cohortcharacterization.domain.CcParamEntity;
import org.ohdsi.webapi.cohortcharacterization.domain.CcStrataEntity;
import org.ohdsi.webapi.cohortcharacterization.domain.CohortCharacterizationEntity;
import org.ohdsi.webapi.cohortcharacterization.dto.BaseCcDTO;
import org.ohdsi.webapi.cohortdefinition.CohortDefinitionEntity;
import org.ohdsi.webapi.cohortdefinition.CohortMetadataExt;
import org.ohdsi.webapi.feanalysis.domain.FeAnalysisEntity;
import org.ohdsi.webapi.feanalysis.domain.FeAnalysisWithCriteriaEntity;
import org.ohdsi.webapi.feanalysis.domain.FeAnalysisWithStringEntity;
import org.ohdsi.webapi.feanalysis.dto.FeAnalysisShortDTO;
import org.ohdsi.webapi.util.ConversionUtils;
import org.springframework.core.convert.ConversionService;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.ohdsi.analysis.cohortcharacterization.design.StandardFeatureAnalysisType.CRITERIA_SET;

public abstract class BaseCcDTOToCcEntityConverter<T extends BaseCcDTO<? extends CohortMetadataExt, ? extends FeAnalysisShortDTO>> {

    private final TempConverterUtils converterUtils;
    private final ConversionService conversionService;

    protected BaseCcDTOToCcEntityConverter(TempConverterUtils converterUtils, ConversionService conversionService) {
        this.converterUtils = converterUtils;
        this.conversionService = conversionService;
    }

    public void baseConvert(T source, CohortCharacterizationEntity target) {
        target.setName(StringUtils.trim(source.getName()));
        target.setDescription(source.getDescription());
        target.setStratifiedBy(source.getStratifiedBy());
        target.setStrataOnly(source.getStrataOnly());

        target.setId(source.getId());

        target.setCohortDefinitions(
                converterUtils.convertSet(source.getCohorts(), CohortDefinitionEntity.class));

        source.getFeatureAnalyses().forEach(fa -> {
            // Legacy Criteria Analyses didn't have statType, they were always PREVALENCE
            if (Objects.equals(fa.getType(), CRITERIA_SET) && fa.getStatType() == null) {
                fa.setStatType(CcResultType.PREVALENCE);
            }
        });
        target.setFeatureAnalyses(convertFeatureAnalyses(source.getFeatureAnalyses()));

        target.setParameters(converterUtils.convertSet(source.getParameters(), CcParamEntity.class));
        target.setStratas(converterUtils.convertSet(source.getStratas(), CcStrataEntity.class));

        CcStrataConceptSetEntity conceptSetEntity = new CcStrataConceptSetEntity();
        conceptSetEntity.setCohortCharacterization(target);
        conceptSetEntity.setRawExpression(Utils.serialize(source.getStrataConceptSets()));
        target.setConceptSetEntity(conceptSetEntity);

        ConversionUtils.convertTags(source, target);
    }

    private Set<FeAnalysisEntity> convertFeatureAnalyses(Collection<? extends FeAnalysisShortDTO> source) {
        var intermediateStream = source.stream()
                .map(item -> {
                    if (Objects.equals(item.getType(), CRITERIA_SET)) {
                        return conversionService.convert(item, FeAnalysisWithCriteriaEntity.class);
                    } else {
                        return conversionService.convert(item, FeAnalysisWithStringEntity.class);
                    }
                });
        return intermediateStream.collect(Collectors.toSet());
    }

}
