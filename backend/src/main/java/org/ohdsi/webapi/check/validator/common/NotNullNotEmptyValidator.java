package org.ohdsi.webapi.check.validator.common;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import org.ohdsi.webapi.check.validator.Context;
import org.ohdsi.webapi.check.validator.Path;
import org.ohdsi.webapi.check.validator.Validator;
import org.ohdsi.webapi.check.warning.WarningSeverity;

public class NotNullNotEmptyValidator<T> extends Validator<T> {
    private static final String NULL_OR_EMPTY = "null or empty";

    public NotNullNotEmptyValidator(Path path, WarningSeverity severity, String errorMessage) {
        super(path, severity, errorMessage);
    }

    @Override
    public boolean validate(T value, Context context) {
        var isValid = true;
        if (Objects.isNull(value)) {
            isValid = false;
        } else {
            if (value instanceof Collection<?> collectionValue) {
                isValid = !collectionValue.isEmpty();
            } else if (value instanceof String stringValue) {
                isValid = !stringValue.isEmpty();
            } else if (value.getClass().isArray()) {
                isValid = Array.getLength(value) > 0;
            } else if (value instanceof Map<?,?> mapValue) {
                isValid = !mapValue.isEmpty();
            }
        }
        if (!isValid) {
            context.addContextWarning(getSeverity(), getErrorMessage(value), path);
        }
        return isValid;
    }

    protected String getDefaultErrorMessage() {
        return NULL_OR_EMPTY;
    }
}
