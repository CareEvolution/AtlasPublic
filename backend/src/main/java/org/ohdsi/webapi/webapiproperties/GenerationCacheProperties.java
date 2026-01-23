package org.ohdsi.webapi.webapiproperties;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class GenerationCacheProperties {
    @Positive(message="cache.generation.invalidateAfterDays must be an integer > 0")
    private Integer invalidateAfterDays;

    @Positive(message="cache.generation.cleanupIntervalMs must be an integer > 0")
    private Integer cleanupIntervalMs;

    @NotNull(message="cache.generation.useAsync must have a value")
    private Boolean useAsync;

    public Integer getInvalidateAfterDays() {
        return invalidateAfterDays;
    }
    public Integer invalidateAfterDays() {
        return invalidateAfterDays;
    }
    public void setInvalidateAfterDays(Integer invalidateAfterDays) {
        this.invalidateAfterDays = invalidateAfterDays;
    }

    public Integer getCleanupIntervalMs() {
        return cleanupIntervalMs;
    }
    public Integer cleanupIntervalMs() {
        return cleanupIntervalMs;
    }
    public void setCleanupIntervalMs(Integer cleanupIntervalMs) {
        this.cleanupIntervalMs = cleanupIntervalMs;
    }

    public Boolean getUseAsync() {
        return useAsync;
    }
    public Boolean useAsync() {
        return useAsync;
    }
    public void setUseAsync(Boolean useAsync) {
        this.useAsync = useAsync;
    }
}
