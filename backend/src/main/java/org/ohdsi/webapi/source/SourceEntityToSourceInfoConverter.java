package org.ohdsi.webapi.source;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class SourceEntityToSourceInfoConverter implements Converter<SourceEntity, SourceInfo> {

    @Override
    public SourceInfo convert(SourceEntity source) {
        var sourceInfo = new SourceInfo(source);
        return sourceInfo;
    }
}
