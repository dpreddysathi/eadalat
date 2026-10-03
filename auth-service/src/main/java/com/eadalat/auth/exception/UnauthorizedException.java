package com.eadalat.auth.exception;

/** Thrown on bad credentials or unauthenticated access. Mapped to 401. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
