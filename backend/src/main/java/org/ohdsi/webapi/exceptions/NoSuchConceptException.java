package org.ohdsi.webapi.exceptions;

public class NoSuchConceptException extends RuntimeException {
    public NoSuchConceptException(String message) {
        super(message);
    }
}
