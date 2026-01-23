package org.ohdsi.webapi.tag.converter;

import jakarta.persistence.Converter;
import org.ohdsi.webapi.tag.domain.TagType;

import jakarta.persistence.AttributeConverter;

@Converter
public class TagTypeConverter implements AttributeConverter<TagType, Integer> {
    @Override
    public Integer convertToDatabaseColumn(TagType tagType) {
        return tagType.getValue();
    }

    @Override
    public TagType convertToEntityAttribute(Integer tagTypeValue) {
        return TagType.fromValue(tagTypeValue);
    }
}
