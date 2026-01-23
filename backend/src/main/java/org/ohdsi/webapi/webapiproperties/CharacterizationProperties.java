package org.ohdsi.webapi.webapiproperties;

import jakarta.validation.constraints.NotNull;

public class CharacterizationProperties {
    @NotNull
    private String organizationName;

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String organizationName() {
        return organizationName;
    }
}
