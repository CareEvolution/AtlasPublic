package org.ohdsi.webapi.cohortdefinition.converter;

import org.ohdsi.webapi.cohortdefinition.CohortDefinitionEntity;
import org.ohdsi.webapi.cohortdefinition.CohortDefinitionDetails;
import org.ohdsi.webapi.cohortdefinition.CohortDefinitionRepository;
import org.ohdsi.webapi.cohortdefinition.dto.CohortRawDTO;
import org.ohdsi.webapi.cohortdefinition.dto.CohortVersionFullDTO;
import org.ohdsi.webapi.util.ExceptionUtils;
import org.ohdsi.webapi.versioning.VersionToVersionDTOConverter;
import org.ohdsi.webapi.versioning.domain.CohortVersion;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CohortVersionToCohortVersionFullDTOConverter implements Converter<CohortVersion, CohortVersionFullDTO> {

    private final CohortDefinitionRepository cohortDefinitionRepository;
    private final ConversionService conversionService;

    public CohortVersionToCohortVersionFullDTOConverter(@Lazy CohortDefinitionRepository cohortDefinitionRepository, @Lazy ConversionService conversionService) {
        this.cohortDefinitionRepository = cohortDefinitionRepository;
        this.conversionService = conversionService;
    }

    @Override
    public CohortVersionFullDTO convert(CohortVersion source) {
        CohortDefinitionEntity def = this.cohortDefinitionRepository.findOneWithDetail(source.getAssetId().intValue());
        ExceptionUtils.throwNotFoundExceptionIfNull(def,
                String.format("There is no cohort definition with id = %d.", source.getAssetId()));

        CohortDefinitionDetails details = new CohortDefinitionDetails();
        details.setExpression(source.getAssetJson());

        CohortDefinitionEntity entity = new CohortDefinitionEntity();
        entity.setId(def.getId());
        entity.setTags(def.getTags());
        entity.setName(def.getName());
        entity.setExpressionType(def.getExpressionType());
        entity.setDetails(details);
        entity.setCohortAnalysisGenerationInfoList(def.getCohortAnalysisGenerationInfoList());
        entity.setGenerationInfoList(def.getGenerationInfoList());
        entity.setCreatedBy(def.getCreatedBy());
        entity.setCreatedDate(def.getCreatedDate());
        entity.setModifiedBy(def.getModifiedBy());
        entity.setModifiedDate(def.getModifiedDate());

        entity.setDescription(source.getDescription());

        CohortVersionFullDTO target = new CohortVersionFullDTO();
        target.setVersionDTO(VersionToVersionDTOConverter.convert(source));
        target.setEntityDTO(conversionService.convert(entity, CohortRawDTO.class));

        return target;
    }
}
