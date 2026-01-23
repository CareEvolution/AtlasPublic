package org.ohdsi.webapi.webapiproperties;

import jakarta.validation.constraints.NotNull;

public class PersonProperties {
    @NotNull(message="webapi.person.view-dates must have a value")
    private Boolean viewDates;

    public Boolean getViewDates() {
        return viewDates;
    }
    public Boolean viewDates() {
        return viewDates;
    }
    public void setViewDates(Boolean viewDates) {
        this.viewDates = viewDates;
    }
}
