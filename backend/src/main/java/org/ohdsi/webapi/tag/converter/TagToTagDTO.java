package org.ohdsi.webapi.tag.converter;

import org.ohdsi.webapi.tag.domain.Tag;
import org.ohdsi.webapi.tag.dto.TagDTO;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;


import java.util.HashSet;

@Component
public class TagToTagDTO implements Converter<Tag, TagDTO> {

    private final ConversionService conversionService;

    public TagToTagDTO(@Lazy ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public TagDTO convert(Tag source) {
        var target = new TagDTO();
        target.setId(source.getId());

        var tagDtos = new HashSet<TagDTO>();
        for (var t : source.getGroups()) {
            var dto = conversionService.convert(t, TagDTO.class);
            tagDtos.add(dto);
        }
        target.setGroups(tagDtos);

        target.setName(source.getName());
        target.setType(source.getType());
        target.setCount(source.getCount());
        target.setColor(source.getColor());
        target.setIcon(source.getIcon());
        target.setShowGroup(source.isShowGroup());
        target.setPermissionProtected(source.isPermissionProtected());
        target.setMultiSelection(source.isMultiSelection());
        target.setMandatory(source.isMandatory());
        target.setAllowCustom(source.isAllowCustom());
        target.setDescription(source.getDescription());

        return target;
    }
}
