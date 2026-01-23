package org.ohdsi.webapi.webapiproperties;

import jakarta.validation.constraints.NotNull;

public class CdmResultsAchillesProperties {
    @NotNull(message="webapi.cdm-results.achilles.enable-cache-warming must have a value")
    Boolean enableCacheWarming;

    public Boolean getEnableCacheWarming() {return enableCacheWarming;}
    public Boolean enableCacheWarming() {return enableCacheWarming;}
    public void setEnableCacheWarming(Boolean enableCacheWarming) {this.enableCacheWarming = enableCacheWarming;}
}
