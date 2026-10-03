package com.eadalat.auth.exception;

/** Thrown when an e-mail address is already registered. Mapped to 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
