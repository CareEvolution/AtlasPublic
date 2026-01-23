package org.ohdsi.webapi.common.generation;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CommonGenerationToDtoConverter implements Converter<CommonGeneration, CommonGenerationDTO> {

    public CommonGenerationDTO convert(final CommonGeneration source) {
        final var resultObject = new CommonGenerationDTO();
        resultObject.setId(source.getId());
        resultObject.setStatus(source.getStatus());
        resultObject.setSourceKey(source.getSource().getSourceKey());
        resultObject.setHashCode(source.getHashCode());
        resultObject.setStartTime(source.getStartTime());
        resultObject.setEndTime(source.getEndTime());
        resultObject.setExitMessage(source.getExitMessage());
        return resultObject;
    }
}
