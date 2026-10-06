package com.researchassistant.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for exceptions that map directly to an HTTP status.
 * GlobalExceptionHandler turns these into {"message": "..."} responses.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    protected ApiException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
