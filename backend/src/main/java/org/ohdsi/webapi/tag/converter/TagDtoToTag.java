package org.ohdsi.webapi.tag.converter;

import org.ohdsi.webapi.tag.domain.Tag;
import org.ohdsi.webapi.tag.dto.TagDTO;

import java.util.HashSet;

public class TagDtoToTag {
    public static Tag convert(TagDTO source) {
        var target = new Tag();
        target.setId(source.getId());

        var tags = new HashSet<Tag>();
        for (var dto : source.getGroups()) {
            var tag = TagDtoToTag.convert(dto);
            tags.add(tag);
        }
        target.setGroups(tags);

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
