package com.eadalat.auth.exception;

/** Thrown when the requester lacks permission. Mapped to 403. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
