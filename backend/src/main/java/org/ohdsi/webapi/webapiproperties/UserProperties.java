package org.ohdsi.webapi.webapiproperties;

import java.util.ArrayList;

public class UserProperties {
    private ArrayList<String> defaultRoles;

    public ArrayList<String> getDefaultRoles() {
        return defaultRoles;
    }
    public ArrayList<String> defaultRoles() {
        return defaultRoles;
    }
    public void setDefaultRoles(ArrayList<String> defaultRoles) {
        this.defaultRoles = defaultRoles;
    }
}
