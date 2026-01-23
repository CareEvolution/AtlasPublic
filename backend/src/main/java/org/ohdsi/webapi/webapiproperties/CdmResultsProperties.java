package org.ohdsi.webapi.webapiproperties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

public class CdmResultsProperties {
    @Valid
    @NestedConfigurationProperty
    public CdmResultsAchillesProperties achilles = new CdmResultsAchillesProperties();

    @NotNull(message="webapi.cdm-results.enable-cache-warming must have a value")
    Boolean enableCacheWarming;

    @Positive(message="webapi.cdm-results.jobs-count must be a positive integer")
    Integer jobsCount;

    public CdmResultsAchillesProperties getAchilles() {return achilles;}
    public CdmResultsAchillesProperties achilles() {return achilles;}
    public void setAchilles(CdmResultsAchillesProperties achilles) {this.achilles = achilles;}

    public Boolean getEnableCacheWarming() {return enableCacheWarming;}
    public Boolean enableCacheWarming() {return enableCacheWarming;}
    public void setEnableCacheWarming(Boolean enableCacheWarming) {this.enableCacheWarming = enableCacheWarming;}

    public Integer getJobsCount() {return jobsCount;}
    public Integer jobsCount() {return jobsCount;}
    public void setJobsCount(Integer jobsCount) {this.jobsCount = jobsCount;}
}
