package org.ohdsi.webapi.cohortcharacterization.converter;

import org.ohdsi.analysis.Utils;
import org.ohdsi.webapi.cohortcharacterization.domain.CohortCharacterizationEntity;
import org.ohdsi.webapi.cohortcharacterization.dto.CcVersionFullDTO;
import org.ohdsi.webapi.cohortcharacterization.dto.CohortCharacterizationDTO;
import org.ohdsi.webapi.cohortcharacterization.repository.CcRepository;
import org.ohdsi.webapi.cohortcharacterization.specification.CohortCharacterizationImpl;
import org.ohdsi.webapi.cohortdefinition.CohortDefinitionService;
import org.ohdsi.webapi.cohortdefinition.dto.CohortMetadataDTO;
import org.ohdsi.webapi.exceptions.FatalException;
import org.ohdsi.webapi.versioning.VersionToVersionDTOConverter;
import org.ohdsi.webapi.versioning.domain.CharacterizationVersion;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import jakarta.ws.rs.NotFoundException;

import java.util.HashSet;
import java.util.stream.Collectors;

@Component
public class CharacterizationVersionToCharacterizationVersionFullDTOConverter
        implements Converter<CharacterizationVersion, CcVersionFullDTO> {

    private final CohortDefinitionService cohortDefinitionService;
    private final CcRepository ccRepository;
    private final ConversionService conversionService;

    // The @Lazy is necessary. Otherwise the bean is newed up too late and doesn't get attached to the ConversionService, 
    // leading to "No converter found" exceptions
    public CharacterizationVersionToCharacterizationVersionFullDTOConverter(
        @Lazy CohortDefinitionService cohortService,
        @Lazy CcRepository ccRepository,
        @Lazy ConversionService conversionService
    ) {
        this.cohortDefinitionService = cohortService;
        this.ccRepository = ccRepository;
        this.conversionService = conversionService;
    }

    public CcVersionFullDTO convert(CharacterizationVersion source) {
        var assetId = source.getAssetId();
        var optionalDef = ccRepository.findById(assetId);
        if (optionalDef.isEmpty()) {
            throw new NotFoundException(
                    String.format("Could not load version %d because it does not exist", assetId));
        }
        var def = optionalDef.get();

        var characterizationImpl =
                Utils.deserialize(source.getAssetJson(), CohortCharacterizationImpl.class);
        var entity = conversionService.convert(characterizationImpl,
                CohortCharacterizationEntity.class);
        entity.setId(def.getId());
        entity.setTags(def.getTags());
        entity.setName(def.getName());
        entity.setCreatedBy(def.getCreatedBy());
        entity.setCreatedDate(def.getCreatedDate());
        entity.setModifiedBy(def.getModifiedBy());
        entity.setModifiedDate(def.getModifiedDate());

        var ids = characterizationImpl.getCohorts().stream()
                .map(CohortMetadataDTO::getId)
                .collect(Collectors.toList());
        var cohortDefinitions = cohortDefinitionService.getCohortDefinitions(ids);
        if (cohortDefinitions.size() != ids.size()) {
            throw new FatalException("Could not load version because it contains deleted cohort definitions");
        }
        entity.setCohortDefinitions(new HashSet<>(cohortDefinitions));

        var target = new CcVersionFullDTO();
        target.setVersionDTO(VersionToVersionDTOConverter.convert(source));
        target.setEntityDTO(conversionService.convert(entity, CohortCharacterizationDTO.class));

        return target;
    }
}
