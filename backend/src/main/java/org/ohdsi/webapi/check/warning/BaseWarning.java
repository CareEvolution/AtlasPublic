package org.ohdsi.webapi.check.warning;

public abstract class BaseWarning implements Warning {

    private final WarningSeverity severity;

    public BaseWarning(WarningSeverity severity) {
        this.severity = severity;
    }

    @Override
    public WarningSeverity getSeverity() {
        return severity;
    }
}
