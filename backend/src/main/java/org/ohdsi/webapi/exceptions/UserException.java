package org.ohdsi.webapi.exceptions;

public class UserException extends RuntimeException {

    public UserException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserException(String message) {
        super(message);
    }

    public UserException(Throwable ex) {
        super(ex);
    }
}
