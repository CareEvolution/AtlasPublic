package org.ohdsi.webapi.cohortcharacterization.converter;

import org.ohdsi.analysis.Utils;
import org.ohdsi.webapi.cohortcharacterization.domain.CohortCharacterizationEntity;
import org.ohdsi.webapi.cohortcharacterization.specification.CohortCharacterizationImpl;
import org.ohdsi.webapi.util.ExportUtil;
import org.ohdsi.webapi.versioning.domain.CharacterizationVersion;
import org.ohdsi.webapi.webapiproperties.WebApiProperties;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CharacterizationToCharacterizationVersionConverter
        implements Converter<CohortCharacterizationEntity, CharacterizationVersion> {

    private final WebApiProperties webApiProperties;
    private final ConversionService conversionService;

    // The @Lazy is necessary. Otherwise the bean is newed up too late and doesn't get attached to the ConversionService, 
    // leading to "No converter found" exceptions
    public CharacterizationToCharacterizationVersionConverter(@Lazy WebApiProperties webApiProperties, @Lazy ConversionService conversionService) {
        this.webApiProperties = webApiProperties;
        this.conversionService = conversionService;
    }

    public CharacterizationVersion convert(CohortCharacterizationEntity source) {
        var characterizationImpl = conversionService.convert(source,
                CohortCharacterizationImpl.class);
        ExportUtil.clearCreateAndUpdateInfo(characterizationImpl);
        characterizationImpl.getFeatureAnalyses().forEach(ExportUtil::clearCreateAndUpdateInfo);
        characterizationImpl.getCohorts().forEach(ExportUtil::clearCreateAndUpdateInfo);
        characterizationImpl.setOrganizationName(webApiProperties.characterization().getOrganizationName());

        String expression = Utils.serialize(characterizationImpl, true);

        CharacterizationVersion target = new CharacterizationVersion();
        target.setAssetId(source.getId());
        target.setAssetJson(expression);

        return target;
    }
}
