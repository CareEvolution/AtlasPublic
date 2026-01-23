package org.ohdsi.webapi.webapiproperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class I18nProperties {
    @NotBlank(message="webapi.i18n.default-locale must have a value")
    private String defaultLocale;

    @NotNull(message="webapi.i18n.enabled must have a value")
    private Boolean enabled;

    public String getDefaultLocale() {
        return defaultLocale;
    }
    public String defaultLocale() {
        return defaultLocale;
    }
    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public Boolean getEnabled() {
        return enabled;
    }
    public Boolean enabled() {
        return enabled;
    }
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
