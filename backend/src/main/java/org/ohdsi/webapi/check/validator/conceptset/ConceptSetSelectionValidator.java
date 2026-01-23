package org.ohdsi.webapi.check.validator.conceptset;

import org.ohdsi.circe.cohortdefinition.ConceptSetSelection;
import org.ohdsi.webapi.check.validator.Context;
import org.ohdsi.webapi.check.validator.Path;
import org.ohdsi.webapi.check.validator.Validator;
import org.ohdsi.webapi.check.warning.WarningSeverity;

import java.util.Objects;

public class ConceptSetSelectionValidator<T extends ConceptSetSelection> extends Validator<T> {
    private static final String EMPTY = "empty";

    public ConceptSetSelectionValidator(Path path, WarningSeverity severity, String errorMessage) {
        super(path, severity, errorMessage);
    }

    @Override
    public boolean validate(T value, Context context) {
        if (Objects.nonNull(value) && Objects.isNull(value.codesetId)) {
            context.addContextWarning(getSeverity(), getErrorMessage(value), path);
            return false;
        }
        return true;
    }

    protected String getDefaultErrorMessage() {
        return EMPTY;
    }
}
