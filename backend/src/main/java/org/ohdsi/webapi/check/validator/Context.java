package org.ohdsi.webapi.check.validator;

import java.util.ArrayList;
import java.util.List;

import org.ohdsi.webapi.check.warning.ContextWarning;
import org.ohdsi.webapi.check.warning.WarningSeverity;

public class Context {
    private final List<ContextWarning> warnings = new ArrayList<>();

    public void addContextWarning(WarningSeverity severity, String message, Path path) {
        warnings.add(new ContextWarning(severity, message, path));
    }

    public List<ContextWarning> getContextWarnings() {
        return warnings;
    }
}
