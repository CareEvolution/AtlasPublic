package org.ohdsi.webapi.webapiproperties;

import jakarta.validation.constraints.NotNull;

public class AuditTrailProperties {
    @NotNull(message = "webapi.audit-trail.enabled must have a value")
    private Boolean enabled;

    //@URL(message = "webapi.audit-trail.log-file in an invalid URL")
    private String logFile;

    //@URL(message = "webapi.audit-trail.extra-log-file in an invalid URL")
    private String extraLogFile;

    public Boolean getEnabled() {
        return enabled;
    }
    public Boolean enabled() {
        return enabled;
    }
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getLogFile() {
        return logFile;
    }
    public String logFile() {
        return logFile;
    }
    public void setLogFile(String logFile) {
        this.logFile = logFile;
    }

    public String getExtraLogFile() {
        return extraLogFile;
    }
    public String extraLogFile() {
        return extraLogFile;
    }
    public void setExtraLogFile(String extraLogFile) {
        this.extraLogFile = extraLogFile;
    }
}
