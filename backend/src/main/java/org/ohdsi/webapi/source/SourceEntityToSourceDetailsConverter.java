package org.ohdsi.webapi.source;

import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class SourceEntityToSourceDetailsConverter implements Converter<SourceEntity, SourceDetails> {
    private AuthorizationManager authorizationManager;

    public SourceEntityToSourceDetailsConverter(@Lazy AuthorizationManager authorizationManager) {
        this.authorizationManager = authorizationManager;
    }

    @Override
    public SourceDetails convert(@NonNull SourceEntity source) {
        var maskSensitiveInfo = !authorizationManager.canReadSensitiveInfo();
        var sourceDetails = new SourceDetails(source, maskSensitiveInfo);
        return sourceDetails;
    }
}
