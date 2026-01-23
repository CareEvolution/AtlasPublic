package org.ohdsi.webapi.check.warning;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DefaultWarning extends BaseWarning {

    private final String message;

    public DefaultWarning(WarningSeverity severity, String message) {
        super(severity);
        this.message = message;
    }

    @Override
    @JsonProperty("message")
    public String toMessage() {
        return message;
    }
}
