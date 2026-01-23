package org.ohdsi.webapi.cdmresults;

import org.ohdsi.webapi.cdmresults.service.CDMCacheService;
import org.ohdsi.webapi.source.SourceEntity;
import org.springframework.stereotype.Component;

@Component
public class CdmResultsCacheTaskletFactory {
    private final CDMCacheService cdmCacheService;

    public CdmResultsCacheTaskletFactory(CDMCacheService cdmCacheService) {
        this.cdmCacheService = cdmCacheService;
    }

    public CdmResultsCacheTasklet create(SourceEntity source) {
        return new CdmResultsCacheTasklet(source, cdmCacheService);
    }
}
