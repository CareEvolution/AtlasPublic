package org.ohdsi.webapi.check.checker.tag.helper;

import org.ohdsi.webapi.check.builder.ValidatorGroupBuilder;
import org.ohdsi.webapi.check.builder.tag.MandatoryTagValidatorBuilder;
import org.ohdsi.webapi.check.warning.WarningSeverity;
import org.ohdsi.webapi.commondto.TaggableEntityDTO;
import org.ohdsi.webapi.tag.TagService;
import org.ohdsi.webapi.tag.dto.TagDTO;
import org.ohdsi.webapi.webapiproperties.WebApiProperties;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class TagHelper<T extends TaggableEntityDTO> {
    private final TagService tagService;
    private final WebApiProperties webApiProperties;

    public TagHelper(TagService tagService, WebApiProperties webApiProperties) {
        this.tagService = tagService;
        this.webApiProperties = webApiProperties;
    }

    public ValidatorGroupBuilder<T, Collection<? extends TagDTO>> prepareTagBuilder() {
        ValidatorGroupBuilder<T, Collection<? extends TagDTO>> builder = new ValidatorGroupBuilder<T, Collection<? extends TagDTO>>()
                .attrName("Tags")
                .conditionGetter(t -> webApiProperties.tag().enabled())
                .severity(WarningSeverity.WARNING)
                .valueGetter(T::getTags)
                .validators(
                        new MandatoryTagValidatorBuilder<>(tagService));
        return builder;
    }
}
