package org.ohdsi.webapi.util;

import org.ohdsi.webapi.model.SecuredEntity;
import org.ohdsi.webapi.model.TaggableEntity;
import org.ohdsi.webapi.tag.converter.TagDtoToTag;
import org.ohdsi.webapi.tag.domain.Tag;
import org.ohdsi.webapi.tag.dto.TagDTO;
import org.ohdsi.webapi.authorization.converter.UserEntityToUserDTOConverter;
import org.ohdsi.webapi.commondto.SecuredEntityDTO;
import org.ohdsi.webapi.commondto.TaggableEntityDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ConversionUtils {

    private static ConversionService conversionService;

    @Autowired
    public void setConversionService(ConversionService conversionService) {
        ConversionUtils.conversionService = conversionService;
    }

    public static void convertMetadata(SecuredEntity<? extends Number> source, SecuredEntityDTO target) {
        target.setCreatedBy(UserEntityToUserDTOConverter.convert(source.getCreatedBy()));
        target.setCreatedDate(source.getCreatedDate());
        target.setModifiedBy(UserEntityToUserDTOConverter.convert(source.getModifiedBy()));
        target.setModifiedDate(source.getModifiedDate());
    }

    public static void convertMetadataAndTags(TaggableEntity<? extends Number> source, TaggableEntityDTO target) {
        if (Objects.nonNull(source.getTags())) {
            Set<TagDTO> tags = new HashSet<>();
            source.getTags().forEach(tag -> {
                var tagDTO = conversionService.convert(tag, TagDTO.class);
                tags.add(tagDTO);
            });
            target.setTags(tags);
        }

        convertMetadata(source, target);
    }

    public static void convertTags(TaggableEntityDTO source, TaggableEntity<? extends Number> target) {
        if (Objects.nonNull(source.getTags())) {
            Set<Tag> tags = source.getTags().stream()
                    .map(tag -> TagDtoToTag.convert(tag)).collect(Collectors.toSet());
            target.setTags(tags);
        }
    }
}
