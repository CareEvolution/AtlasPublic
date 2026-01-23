package org.ohdsi.webapi.webapiproperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CdmDbProperties {
    @NotNull(message = "webapi.cdm-db.jdbc-template-suppress-api-exceptions must have a value")
    private Boolean jdbcTemplateSuppressApiExceptions;
    @NotBlank(message = "webapi.cdm-db.version must have a value")
    private String version;

    public Boolean getJdbcTemplateSuppressApiExceptions() {return jdbcTemplateSuppressApiExceptions;}
    public void setJdbcTemplateSuppressApiExceptions(Boolean jdbcTemplateSuppressApiExceptions) {this.jdbcTemplateSuppressApiExceptions = jdbcTemplateSuppressApiExceptions;}
    public Boolean jdbcTemplateSuppressApiExceptions() {return jdbcTemplateSuppressApiExceptions;}

    public String getVersion() {
        return version;
    }

    public String version() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}


