package org.ohdsi.webapi.webapiproperties;

import jakarta.validation.constraints.NotNull;

public class CorsProperties {
    @NotNull(message="cors.enabled must have a value")
    Boolean enabled;

    String[] allowedOrigins;

    public Boolean getEnabled() {
        return enabled;
    }
    public Boolean enabled() {
        return enabled;
    }
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    public String[] getAllowedOrigins() {
        return allowedOrigins;
    }
    public void setAllowedOrigins(String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
    public String[] allowedOrigins() {
        return allowedOrigins;
    }
}
