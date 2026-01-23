package org.ohdsi.webapi.exceptions;

public class FatalException extends RuntimeException {
    public FatalException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public FatalException(String msg) {
        super(msg);
    }

    public FatalException(Throwable cause) {
        super(cause);
    }
}
