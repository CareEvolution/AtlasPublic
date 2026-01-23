package org.ohdsi.webapi.check.warning;

import org.ohdsi.webapi.check.validator.Path;

public record ContextWarning(WarningSeverity severity, String message, Path path) {
}
