package com.eadalat.auth.exception;

/** Thrown when a record cannot be found. Mapped to 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
