package org.ohdsi.webapi.webapiproperties;

import jakarta.validation.constraints.NotBlank;

public class AppDbProperties {

    @NotBlank(message = "app-db.flyway-password cannot be blank")
    private String flywayPassword;

    @NotBlank(message = "app-db.flyway-username cannot be blank")
    private String flywayUsername;

    @NotBlank(message = "app-db.password cannot be blank")
    private String password;

    @NotBlank(message = "app-db.schemaName cannot be blank")
    private String schemaName;

    // Ideally we could create a custom "JDBC URL" annotation.  The stock
    // URL validator (from Hibernate) will not work because it can't handle
    // the leading jdbc: stuff.
    @NotBlank(message = "app-db.url cannot be blank")
    private String url;

    @NotBlank(message = "app-db.username cannot be blank")
    private String username;

    public String flywayPassword() {
        return flywayPassword;
    }

    public String getFlywayPassword() {
        return flywayPassword;
    }

    public void setFlywayPassword(String flywayPassword) {
        this.flywayPassword = flywayPassword;
    }

    public String getFlywayUsername() {
        return flywayUsername;
    }

    public String flywayUsername() {
        return flywayUsername;
    }

    public void setFlywayUsername(String flywayUsername) {
        this.flywayUsername = flywayUsername;
    }

    public String getPassword() {
        return password;
    }
    public String password() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public String getSchemaName() {
        return schemaName;
    }
    public String schemaName() {
        return schemaName;
    }
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getUrl() {
        return url;
    }
    public String url() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }
    public String username() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
}
